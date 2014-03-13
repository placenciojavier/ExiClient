package cl.clayster.exi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.xerces.impl.dv.util.Base64;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.ObservableReader;
import org.jivesoftware.smack.util.ObservableWriter;
import org.xml.sax.SAXException;

import com.siemens.ct.exi.exceptions.EXIException;

/**
 * Defines necessary methods to establish an EXI connection over XMPP according to XEP-0322
 * 
 * @author Javier Placencio
 *
 */
public class EXIXMPPConnection extends XMPPConnection{
	
	public static final int ABORT_COMPRESSION = -1;
	public static final int UPLOAD_BINARY = 0;
	public static final int UPLOAD_EXI_DOCUMENT = 1;
	public static final int UPLOAD_EXI_BODY = 2;
	public static final int UPLOAD_URL = 3;
	
	private boolean usingEXI = false;
	protected EXISetupConfiguration exiConfig;
	protected EXIBaseProcessor exiProcessor;
	
	protected int schemaDownloadsCounter = 0;
	protected boolean sentMissingSchemas = false;
	
	private int uploadSchemaOption = UPLOAD_BINARY;
	
	/**
	 * This constructor uses the given <code>EXISetupConfiguration</code> to negotiate EXI compression while logging in. 
	 * @param config configurations to connect to the server
	 * @param exiConfig EXI parameters to be used. <b>Default values</b> will be used if exiConfig is null
	 */
	public EXIXMPPConnection(ConnectionConfiguration config, EXISetupConfiguration exiConfig) {
		super(config);
		
		if(exiConfig == null)	exiConfig = new EXISetupConfiguration();
		this.exiConfig = exiConfig;
		if(!new File(EXIUtils.schemasFileLocation).exists())
			try {
				EXIUtils.generateBoth(EXIUtils.schemasFolder, exiConfig);
			} catch (NoSuchAlgorithmException | IOException e1) {
				e1.printStackTrace();
				return;
			}
	}
	
	@Override
	protected boolean useCompression() {
		if(!compressionMethods.contains("exi")){
			System.err.println("The server does not support EXI compression.");
			return false;
		}
		
		// maybe use quick setup
		if(exiConfig.isQuickSetup() && proposeEXICompressionQuickSetup()){
			exiConfig.setQuickSetup(false);
			return true;
		}
		else{
			return proposeEXICompression();
		}
	}
	
	public int getUploadSchemaOption(){
		return uploadSchemaOption;
	}
	
	public void setUploadSchemaOption(int option){
		if(option < -1 || option > 3)	option = UPLOAD_BINARY;
		uploadSchemaOption = option;
	}


	/**
	 * Uses the last configuration in order to skip the handshake. 
	 * @return	<b>true</b> if there is a previous configuration available, <b>false</b> otherwise 
	 */
	public boolean proposeEXICompressionQuickSetup(){		
		String setupStanza = "";
		String configId = Preferences.userRoot().get(EXIUtils.REG_KEY, null);
		boolean quickSetup = false;
		quickSetup = (configId != null); // quickSetup is valid if there is a value in registry or else it is false and normal setup stanza will be sent
		
		if(quickSetup){
			exiConfig = EXIUtils.parseQuickConfigId(configId);
			setupStanza = "<setup xmlns='http://jabber.org/protocol/compress/exi' configurationId='" + configId + "'/>";
			try {
				send(setupStanza);
				return true;
			} catch (IOException e) {
				System.err.println("Error while writing <setup> stanza: " + e.getMessage());
			}
		}
		return false;
	}
	
	/**
	 * Sends an EXI setup proposition to the server (is called only if the compression is supported)
	 */
	public boolean proposeEXICompression(){		
		try {
			String setupStanza = parseSetupStanza();
	        send(setupStanza);
			return true;
		} catch (DocumentException e) {
			System.err.println("Unable to propose EXI compression. " + e.getMessage());
			return false;
		} catch (IOException e) {
			System.err.println("Error while writing <setup> stanza: " + e.getMessage());
			return false;
		}
	}
	
	protected String parseSetupStanza() throws DocumentException{
		Element setupElement = DocumentHelper.parseText(EXIUtils.readFile(EXIUtils.schemasFileLocation)).getRootElement();
 		Element auxSchema;
        for (@SuppressWarnings("unchecked") Iterator<Element> i = setupElement.elementIterator("schema"); i.hasNext();) {
        	auxSchema = i.next();
        	auxSchema.remove(auxSchema.attribute("url"));
        	auxSchema.remove(auxSchema.attribute("schemaLocation"));
        }
        return setupElement.asXML();
	}
	
	protected void send(String message) throws IOException{
		writer.write(message);
		writer.flush();
	}
	
	@Override
	public void requestStreamCompression(String method) {
		if("exi".equalsIgnoreCase(method)){
			try {
				exiProcessor = new EXIProcessor(EXIUtils.canonicalSchemaLocation, exiConfig);
			} catch (EXIException e) {
				System.err.println("Unable to create EXI Processor.");
				return;
			}
		}
		super.requestStreamCompression(method);
	};
	
	public void startStreamCompression1() throws Exception{
		startStreamCompression();
	}
	
	@Override
	protected void startStreamCompression() throws Exception{
		serverAckdCompression = true;
		
		// Very important function set the EXI Processor to the EXIWriter and EXIReader!!
		setEXIProcessor();
		// enable EXIProcessor and send start stream tag
		openEXIStream();
		
		// Notify that compression is being used
	    synchronized (this) {
	        this.notify();
	    }
	}
	
	
	
	

	/**
	 * Send schemas that are missing in the server.
	 * 
	 * @param missingSchemas a list containing all schemas missing in the server
	 * @param opt how missing schemas will be sent to the server. Options are as follows
	 * <br> 1 - upload schema as EXI document
	 * <br> 2 - upload schema as EXI body
	 * <br> 3 - send a url for the server to download the schema by itself  
	 * <br> x - anything else to upload schema as a binary file
	 * @throws TransformerException 
	 * @throws SAXException 
	 * @throws EXIException 
	 * @throws DocumentException 
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 * @throws XMLStreamException 
	 */
	public void sendMissingSchemas(List<String> missingSchemas, int opt) 
			throws NoSuchAlgorithmException, IOException, DocumentException, EXIException, SAXException, TransformerException, XMLStreamException {
		sentMissingSchemas = true;
		switch(opt){
			case ABORT_COMPRESSION:	// abort EXI compression, continue using normal XMPP
System.out.println("EXI compression aborted. Continuing with normal XMPP communication.");//TODO
				break;
			case UPLOAD_EXI_DOCUMENT: // upload compressed EXI document
				uploadCompressedMissingSchemas(missingSchemas, false);
				break;
			case UPLOAD_EXI_BODY: // upload compressed EXI body
				uploadCompressedMissingSchemas(missingSchemas, true);
				break;
			case UPLOAD_URL:	// send URL and download on server 
				downloadSchemas(missingSchemas);
				break;
			default: // upload binary file
				uploadMissingSchemas(missingSchemas);
				break;
		}
	}
	
	@Override
	public boolean isUsingCompression(){
		return this.usingEXI;
	}

	/**
	 * Defines the reader and writer which are capable of sending both normal XMPP messages and EXI messages depending on which one is enabled.
	 */
	@Override
	protected void initReaderAndWriter() throws XMPPException {
		try {
			EXISetupConfiguration quickExiConfig = EXIUtils.parseQuickConfigId(Preferences.userRoot().get(EXIUtils.REG_KEY, null));
			if(quickExiConfig != null){
				exiConfig = quickExiConfig;
			}
		} catch (NumberFormatException e){	
			// error en el formato del configId. Se borra y se intenta nuevamente con blockSize y strict por defecto.
			EXIUtils.saveConfigId(null);
			initReaderAndWriter();
			return;
		}
		try {
            if (compressionHandler == null) {
            	reader = new EXIReader(socket.getInputStream());
                writer = new EXIWriter(socket.getOutputStream());
            }
            else {
                try {
                    OutputStream os = compressionHandler.getOutputStream(socket.getOutputStream());
                    //writer = new EXIWriter(new OutputStreamWriter(os, EXIProcessor.CHARSET), exiProcessor);
                    writer = new EXIWriter(os);

                    InputStream is = compressionHandler.getInputStream(socket.getInputStream());
                    //reader = new EXIReader(new InputStreamReader(is, EXIProcessor.CHARSET), exiProcessor);
                    reader = new EXIReader(is);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    compressionHandler = null;
                    //reader = new EXIReader(new InputStreamReader(socket.getInputStream(), EXIProcessor.CHARSET), exiProcessor);
                    reader = new EXIReader(socket.getInputStream());
                    //writer = new EXIWriter(new OutputStreamWriter(socket.getOutputStream(), EXIProcessor.CHARSET), exiProcessor);
                    writer = new EXIWriter(socket.getOutputStream());
                }
            }
        }
        catch (IOException ioe) {
            throw new XMPPException(
                    "EXI_XMPPError establishing connection with server.",
                    new XMPPError(XMPPError.Condition.remote_server_error,
                            "EXI_XMPPError establishing connection with server."),
                    ioe);
        }
        // If debugging is enabled, we open a window and write out all network traffic.
        initDebugger();
	}
	
	/**
	 * Sets the current exiProcessor in this class as the current EXIProcessor in EXIWriter and EXIReader 
	 */
	private void setEXIProcessor(){
		if(reader instanceof ObservableReader && writer instanceof ObservableWriter){
			((EXIReader) ((ObservableReader) reader).wrappedReader).setExiProcessor(exiProcessor);
			((EXIWriter) ((ObservableWriter) writer).wrappedWriter).setExiProcessor(exiProcessor);
		}
		else if(reader instanceof EXIReader && writer instanceof EXIWriter){
			((EXIReader) reader).setExiProcessor(exiProcessor);
			((EXIWriter) writer).setExiProcessor(exiProcessor);
			//System.out.println("EXIReader and EXIWriter alone (not wrapped into ObservableReader/Writer)");
		}
		
		else System.err.println("Unable to create EXI Processor: Instances of reader and writer are not treated. (EXIXMPPConnection.setEXIProcessor)");
	}

	/**
	 * Turns on the EXI connection to start encoding and decoding EXI messages. Connection parameters should be already negotiated with the server.
	 * 
	 * @param enable true to enable EXI messages (false to disable)
	 * @throws IOException 
	 */
	protected void enableEXI(boolean enable){
		this.usingEXI = enable;
		if(reader instanceof ObservableReader && writer instanceof ObservableWriter){
			((EXIReader) ((ObservableReader) reader).wrappedReader).setEXI(enable);
			((EXIWriter) ((ObservableWriter) writer).wrappedWriter).setEXI(enable);
		}
		else if(reader instanceof EXIReader && writer instanceof EXIWriter){
			((EXIReader) reader).setEXI(enable);
			((EXIWriter) writer).setEXI(enable);
			//System.out.println("EXIReader and EXIWriter alone (not wrapped into ObservableReader/Writer)");
		}	
		else System.err.println("Unable to create EXI Processor: Instances of reader and writer are not treated. (EXIXMPPConnection.enableEXI)");
	}
	
	protected void openEXIStream() throws IOException{
		enableEXI(true);
		String exiStreamStart = "<exi:streamStart from='"
				+ getUser()
	 			+ "' to='"
	 			+ getHost()
	 			+ "' version='1.0'"
	 			+ " xml:lang='en'"
	 			+ " xmlns:exi='http://jabber.org/protocol/compress/exi'>"
	 			+ "<exi:xmlns prefix='' namespace='jabber:client'/>"
	 			+ "<exi:xmlns prefix='streams' namespace='http://etherx.jabber.org/streams'/>"
	 			+ "<exi:xmlns prefix='exi' namespace='http://jabber.org/protocol/compress/exi'/>"
	 			+ "</exi:streamStart>";
		send(exiStreamStart);
	}
	
	

	private void uploadMissingSchemas(List<String> missingSchemas) throws IOException, DocumentException, EXIException, SAXException, TransformerException, NoSuchAlgorithmException {
		String xml, schemaLocation = null;
		String schemasFileContent = EXIUtils.readFile(EXIUtils.schemasFileLocation);
		Element schemasFileElement = DocumentHelper.parseText(schemasFileContent).getRootElement();
		Element auxElement;
		for(String ms : missingSchemas){
			for (@SuppressWarnings("unchecked") Iterator<Element> i = schemasFileElement.elementIterator("schema"); i.hasNext();) {
				auxElement = i.next();
				if(ms.equals(auxElement.attributeValue("ns"))){
					schemaLocation = auxElement.attributeValue("schemaLocation");
					break;
				}
			}
			if(schemaLocation == null){
				System.err.println("error: no se ha encontrado el archivo para " + ms);
				return;
			}
			
			String contentType = "Text", content = Base64.encode(Files.readAllBytes(Paths.get(schemaLocation)));
			
			xml = "<uploadSchema xmlns='http://jabber.org/protocol/compress/exi' contentType='" + contentType + "'>"
					.concat(content)
					.concat("</uploadSchema>");
		
		send(xml);
		}
	}
	
	private void uploadCompressedMissingSchemas(List<String> missingSchemas, boolean exiBody) throws IOException, DocumentException, EXIException, SAXException, TransformerException, NoSuchAlgorithmException, XMLStreamException {
		String schemaLocation = null;
		String schemasFileContent = EXIUtils.readFile(EXIUtils.schemasFileLocation);
		Element schemasFileElement = DocumentHelper.parseText(schemasFileContent).getRootElement();
		Element auxElement;
		
		byte[] ba, xmlStart, xmlEnd = "</uploadSchema>".getBytes();
		BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
		for(String ms : missingSchemas){
			for (@SuppressWarnings("unchecked") Iterator<Element> i = schemasFileElement.elementIterator("schema"); i.hasNext();) {
				auxElement = i.next();
				if(ms.equals(auxElement.attributeValue("ns"))){
					schemaLocation = auxElement.attributeValue("schemaLocation");
					break;
				}
			}
			if(schemaLocation == null){
				System.err.println("error: no se ha encontrado el archivo para " + ms);
				return;
			}
			
			MessageDigest md = MessageDigest.getInstance("MD5");
	    	File file = new File(schemaLocation);
			String md5Hash = EXIUtils.bytesToHex(md.digest(Files.readAllBytes(file.toPath())));
			String archivo = new String(Files.readAllBytes(file.toPath()));
			String contentType = "ExiDocument";

			byte[] content = new byte[]{};
			if(exiBody){
				contentType = "ExiBody";
				content = EXIProcessor.encodeEXIBody(archivo);
			}
			else{
				content = EXIProcessor.encodeSchemaless(archivo, false);
			}
			
			xmlStart = ("<uploadSchema xmlns='http://jabber.org/protocol/compress/exi'"
                    + " contentType='" + contentType + "' md5Hash='" + md5Hash + "' bytes='" + file.length() + "'>").getBytes();                    
			
			ba = new byte[xmlStart.length + content.length + xmlEnd.length];
			System.arraycopy(xmlStart, 0, ba, 0, xmlStart.length);
			System.arraycopy(content, 0, ba, xmlStart.length, content.length);
			System.arraycopy(xmlEnd, 0, ba, xmlStart.length + content.length, xmlEnd.length);
			
System.out.println("Message Content in hex (" + content.length + "): " + EXIUtils.bytesToHex(content));
			bos.write(ba);
			bos.flush();
		}
	}
	
	private void downloadSchemas(List<String> missingSchemas) throws IOException, NoSuchAlgorithmException, DocumentException, EXIException, SAXException, TransformerException{
		String msg = "", url = "";
		Element schemasElement;
		try {
			schemasElement = DocumentHelper.parseText(EXIUtils.readFile(EXIUtils.schemasFileLocation)).getRootElement();
		} catch (DocumentException e) {
			System.err.println("error: no se ha encontrado el archivo: " + EXIUtils.schemasFileLocation);
			return;
		}
		Element auxElement;
		for(String ms : missingSchemas){
			url = "";
			for (@SuppressWarnings("unchecked") Iterator<Element> i = schemasElement.elementIterator("schema"); i.hasNext();) {
				auxElement = i.next();
				if(auxElement.attributeValue("ns").equals(ms)){
					url = auxElement.attributeValue("url");
					break;
				}
			}
			if(!url.equals("")){
				msg = "<downloadSchema xmlns='http://jabber.org/protocol/compress/exi' url='" + url + "'/>";
				send(msg);
				schemaDownloadsCounter++;
			}
			else{
				System.err.println("No url for " + ms + ". Trying to upload schema as binary file.");
				List<String> l = new ArrayList<String>();
				l.add(ms);
				uploadMissingSchemas(l);
			}
		}	
	}
	
	/**
	 * Used to reduce and get the count of schemas being downloaded on the server.
	 * @return Amount of schemas left to be downloaded. When 0 is returned, then the server has downloaded all schemas.
	 */
	public int schemaDownloaded(){
		return this.schemaDownloadsCounter;
	}
	
	public boolean getSentMissingSchemas(){
		return this.sentMissingSchemas;
	}
}

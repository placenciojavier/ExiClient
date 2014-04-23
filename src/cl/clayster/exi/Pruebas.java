package cl.clayster.exi;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.dom4j.DocumentException;
import org.jivesoftware.smack.XMPPException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.siemens.ct.exi.EXIFactory;
import com.siemens.ct.exi.GrammarFactory;
import com.siemens.ct.exi.api.sax.EXIResult;
import com.siemens.ct.exi.api.sax.EXISource;
import com.siemens.ct.exi.exceptions.EXIException;
import com.siemens.ct.exi.grammars.Grammars;


public class Pruebas{ 
	
	public static void main(String[] args) throws XMPPException, IOException, DocumentException, NoSuchAlgorithmException, EXIException, SAXException, TransformerException, ParserConfigurationException{
		/*
		EXISetupConfiguration config = new EXISetupConfiguration();
		long[] enc = new long[20], dec = new long[20];
		EXIProcessor p = new EXIProcessor(config);
		for(int i = 0 ; i < 20 ; i++){
			String xml0 = "<message id='Sk5JM-19' to='exi2@exi.clayster.cl/Spark 2.6.3' from='exi1@exi.clayster.cl/Smack'><body>"
					+ "estas palabras son el mensaje que contiene muchas palabras con palabras y palabras y palabras lalalala palabras</body></message>";
			long ini = System.currentTimeMillis();
			System.out.println("encoding " + ini);
			byte[] exi = p.encodeToByteArray(xml0);
			long fin = System.currentTimeMillis();
			System.out.println("encoded (" + exi.length + "): " + EXIUtils.bytesToHex(exi));
			System.out.println("took: " + (fin-ini) + "[ms]");
			enc[i] = (fin - ini);
			
			ini = System.currentTimeMillis();
			System.out.println("decoding " + ini);
			String xml1 = p.decodeByteArray(exi);
			fin = System.currentTimeMillis();
			System.out.println("decoded (" + xml1.length() + "): " + xml1);
			System.out.println("took: " + (fin-ini) + "[ms]");
			dec[i] = (fin - ini);
		}
		for(int i = 0 ; i < 5 ; i++){
			String xml0 = "<message><failure xmlns='urn:xmpp:iot:sensordata' seqnr='2' done='true'>"
						+ "<error nodeId='Device01' timestamp='2013-03-07T17:13:30'>"
							+ "Timeout."
						+ "</error>"
					+ "</failure></message>";
			long ini = System.currentTimeMillis();
			System.out.println("encoding " + ini);
			byte[] exi = p.encodeToByteArray(xml0);
			long fin = System.currentTimeMillis();
			System.out.println("encoded (" + exi.length + "): " + EXIUtils.bytesToHex(exi));
			System.out.println("took: " + (fin-ini) + "[ms]");
			enc[i] = (fin - ini);
			
			ini = System.currentTimeMillis();
			System.out.println("decoding " + ini);
			String xml1 = p.decodeByteArray(exi);
			fin = System.currentTimeMillis();
			System.out.println("decoded (" + xml1.length() + "): " + xml1);
			System.out.println("took: " + (fin-ini) + "[ms]");
			dec[i] = (fin - ini);
		}
		for(int i = 0 ; i < 20 ; i++){
			String xml0 = "<message id='Sk5JM-19' to='exi2@exi.clayster.cl/Spark 2.6.3' from='exi1@exi.clayster.cl/Smack'><body>"
					+ "estas palabras son el mensaje que contiene muchas palabras con palabras y palabras y palabras lalalala palabras</body></message>";
			long ini = System.currentTimeMillis();
			System.out.println("encoding " + ini);
			byte[] exi = p.encodeToByteArray(xml0);
			long fin = System.currentTimeMillis();
			System.out.println("encoded (" + exi.length + "): " + EXIUtils.bytesToHex(exi));
			System.out.println("took: " + (fin-ini) + "[ms]");
			enc[i] = (fin - ini);
			
			ini = System.currentTimeMillis();
			System.out.println("decoding " + ini);
			String xml1 = p.decodeByteArray(exi);
			fin = System.currentTimeMillis();
			System.out.println("decoded (" + xml1.length() + "): " + xml1);
			System.out.println("took: " + (fin-ini) + "[ms]");
			dec[i] = (fin - ini);
		}
		long promEnc = 0;
		long promDec = 0;
		for(int i = 1 ; i < enc.length ; i++){
			promEnc += enc[i];
			promDec += dec[i];
		}
		promEnc /= enc.length;
		promDec /= dec.length;
		System.out.println("Average encoding time: " + promEnc + "[ms].");
		System.out.println("Average decoding time: " + promDec + "[ms].");
		
		boolean a = true;
		if(a)return;
		_*/
		/*
		byte[] ba = ("1234567890qwertyuiopasdfghjklzxcvbnm"
				   + "098765432poiuytrewq�lkjhgfdsamnbvcxz").getBytes();
		byte[] r = new byte[ba.length];
		ByteArrayInputStream is = new ByteArrayInputStream(ba);
		
		
		is.read(r, 0, ba.length/2);
System.out.println("first half: " + new String(r));
		r = new byte[ba.length];
		if(is.available() > 0){
			is.read(r, 0, ba.length/2);
		}
		else{
			System.err.println("end of the stream");
		}
System.out.println("second half: " + new String(r));
		if(is.available() > 0){
			is.read(r, 0, ba.length/2);
		}
		else{
			System.err.println("end of the stream");
		}
		*/
		
		/** SAX Deocoder **/
		
		EXISetupConfiguration exiConfig = new EXISetupConfiguration();
	    EXIResult exiResult;
	    SAXSource exiSource;
		Transformer transformer;
		EXIFactory exiFactory = exiConfig;
		String xsdLocation = exiConfig.getCanonicalSchemaLocation();
		XMLReader exiReader, xmlReader;
		GrammarFactory grammarFactory = GrammarFactory.newInstance();
        Grammars g = grammarFactory.createGrammars(xsdLocation, new SchemaResolver());
        g.setSchemaId(exiConfig.getSchemaId());
        exiFactory.setGrammars(g);
        
        TransformerFactory tf = TransformerFactory.newInstance();
		transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		
		exiResult = new EXIResult(exiFactory);
        xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setContentHandler(exiResult.getHandler());
		
		exiSource = new EXISource(exiFactory);
        exiReader = exiSource.getXMLReader();
        
        EXIProcessor ep = new EXIProcessor(exiConfig);
		
		byte[] aux = ep.encodeToByteArray("<compressed xmlns='http://jabber.org/protocol/compress'/>");
		byte[] aux2 = ep.encodeToByteArray("<presence id=\"IRqHf-3\"></presence>");
		byte[] ba = new byte[aux.length + aux2.length];
		System.arraycopy(aux, 0, ba, 0, aux.length);
		System.arraycopy(aux2, 0, ba, aux.length, aux2.length);
		
		BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(ba));
		SAXSource saxSource = new SAXSource(new InputSource(bis));
		saxSource.setXMLReader(exiReader);
		
		int av = bis.available();
		try{
			while(bis.available() > 0){			
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				transformer.transform(saxSource, new StreamResult(os));
				System.out.println("DECODED: " + os.toString());
				av = bis.available();
			}
		}catch (TransformerException e){
			System.out.println("listo. av=" + av);
		}
		

		/*
		
		int i = bais.available();
		while(i > 0){
				String xml = ep.decode(bais);
				System.out.println(xml);
				i = bais.available();
		}
		*/
		
		/** DOM Deocoder **/ 
		/*
		is = new ByteArrayInputStream(ba);
		DOMBuilder domBuilder = new DOMBuilder(ef);
		boolean isFragment = ef.isFragment();
		
		OutputStream xmlOutput = new ByteArrayOutputStream();
		
		Node doc;
		if (isFragment) {
			doc = domBuilder.parseFragment(is);
		} else {
			doc = domBuilder.parse(is);
		}
		// create string from xml tree
		StringWriter sw = new StringWriter();
		
		// set up a transformer
		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer trans = transfac.newTransformer();
		// output options
		trans.setOutputProperty(OutputKeys.METHOD, "xml");
		// due to fragments
		trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		// remaining keys
		trans.setOutputProperty(OutputKeys.INDENT, "yes");
		// create string from xml tree
		StreamResult result = new StreamResult(sw);
		DOMSource source = new DOMSource(doc);
		trans.transform(source, result);

		String xmlString = sw.toString();
		
System.out.println("xmlString: " + xmlString);

		xmlOutput.write(xmlString.getBytes());
		
		System.out.println();System.out.println();
		System.out.println("XML message X3:");
		String msg = "<msg><message id='WP9nM-4' to='javier@exi.clayster.cl/Spark' from='exiuser@exi.clayster.cl/Smack' type='chat'><body>hola</body><thread>NP1yo0</thread></message>"
				+ "<message id='WP9nM-4' to='javier@exi.clayster.cl/Spark' from='exiuser@exi.clayster.cl/Smack' type='chat'><body>hola</body><thread>NP1yo0</thread></message>"
				+ "<message id='WP9nM-4' to='javier@exi.clayster.cl/Spark' from='exiuser@exi.clayster.cl/Smack' type='chat'><body>hola</body><thread>NP1yo0</thread></message></msg>";
		System.out.println(EXIUtils.bytesToHex(msg.getBytes()));
		
		System.out.println("EXI message X3:");
		System.out.println(EXIUtils.bytesToHex(ep.encodeToByteArray(msg)));
		*/
	}
	
}




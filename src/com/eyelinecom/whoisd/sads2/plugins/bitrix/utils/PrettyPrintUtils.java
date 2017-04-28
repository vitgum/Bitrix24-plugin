package com.eyelinecom.whoisd.sads2.plugins.bitrix.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * author: Artem Voronov
 */
public class PrettyPrintUtils {

  public static String toPrettyMap(Map<String, String[]> parameters) {
    return parameters.entrySet().stream().map(stringEntry -> "" + stringEntry.getKey() + " == " +stringEntry.getValue()[0]).collect(Collectors.joining("\n"));
  }

  public static String toPrettyXml(String xml) {
    try {
      // Turn xml string into a document
      Document document = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));

      // Remove whitespaces outside tags
      document.normalize();
      XPath xPath = XPathFactory.newInstance().newXPath();
      NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
        document,
        XPathConstants.NODESET);

      for (int i = 0; i < nodeList.getLength(); ++i) {
        Node node = nodeList.item(i);
        node.getParentNode().removeChild(node);
      }

      // Setup pretty print options
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformerFactory.setAttribute("indent-number", 2);
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      // Return pretty print xml string
      StringWriter stringWriter = new StringWriter();
      transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
      return stringWriter.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    System.out.println(toPrettyXml("<?xml version=\"1.0\" encoding=\"utf-8\"?><page version=\"2.0\"><div><input navigationId=\"submit\" name=\"event.text\" title=\"Hello! My name is Артем Воронов. I'll try to help you.\" /></div><navigation id=\"submit\"><link accesskey=\"1\" pageId=\"DEPLOY_URL/bitrix_plugin/bitrix?domain=company.bitrix24.ru&amp;lang=en&amp;redirect_back_page=http%3A%2F%2F212.192.169.150%3A18101%2Fbitrix_plugin%2Fbitrix%3Fdomain%3Dcompany.bitrix24.ru%26amp%3Buser_id%3Db4e57de8-7b9a-45b7-a2b8-5f3c42d0aadc%26amp%3Bservice%3Ddevel.lk.64.1492410911647%26amp%3Bprotocol%3Dvkontaktelang%3Den%26amp%3Bback_page_original%3Dhttp%253A%252F%252Fhosting-api-test.eyeline.mobi%252Findex%253Fsid%253D660%2526amp%253Bsver%253D19%2526amp%253Bpid%253D4%2526amp%253Banswer%253Dback_page\">Ok</link></navigation><navigation><link accesskey=\"2\" pageId=\"DEPLOY_URL/bitrix_plugin/bitrix?domain=company.bitrix24.ru&amp;user_id=b4e57de8-7b9a-45b7-a2b8-5f3c42d0aadc&amp;service=devel.lk.64.1492410911647&amp;protocol=vkontaktelang=en&amp;back_page_original=http%3A%2F%2Fhosting-api-test.eyeline.mobi%2Findex%3Fsid%3D660%26amp%3Bsver%3D19%26amp%3Bpid%3D4%26amp%3Banswer%3Dback_page\">\u2063Back</link></navigation></page>"));
  }
}

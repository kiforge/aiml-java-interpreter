package com.batiaev.aiml.loaders;

import com.batiaev.aiml.consts.AimlConst;
import com.batiaev.aiml.consts.AimlTag;
import com.batiaev.aiml.entity.AimlCategory;
import com.batiaev.aiml.utils.XmlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by anbat on 21/06/15.
 *
 * @author anton
 */
public class AimlLoader {
    private static final Logger LOG = LoggerFactory.getLogger(AimlLoader.class);

    /**
     * Loading all aiml files from folder
     *
     * @param aimlDir folder contain all aiml files
     * @return list of loaded categories
     */
    public List<AimlCategory> loadFiles(String aimlDir) {

        List<AimlCategory> categories = new ArrayList<>();
        File aimls = new File(aimlDir);
        File[] files = aimls.listFiles();
        if (files == null || files.length == 0) {
            LOG.warn("Not files in folder {} ", aimls.getAbsolutePath());
            return categories;
        }
        int countNotAimlFiles = 0;
        for (File file : files) {
            if (file.getName().endsWith(AimlConst.aiml_file_suffix))
                categories.addAll(loadFile(file));
            else
                ++countNotAimlFiles;
        }
        if (countNotAimlFiles != 0)
            LOG.warn("Founded {} not aiml files in folder {}", countNotAimlFiles, aimlDir);
        LOG.info("Loaded {} categories", categories.size());
        return categories;
    }

    /**
     * Loading single aiml file
     *
     * @param aimlFile aiml file
     */
    private List<AimlCategory> loadFile(File aimlFile) {
        Element aimlRoot = XmlHelper.loadXml(aimlFile);
        if (aimlRoot == null)
            return Collections.emptyList();

        if (!aimlRoot.getNodeName().equals(AimlTag.aiml)) {
            LOG.warn(aimlFile.getName() + " is not AIML file");
            return Collections.emptyList();
        }
        String aimlVersion = aimlRoot.getAttribute("version");
        LOG.debug("Load aiml " + aimlFile.getName() + (aimlVersion.isEmpty() ? "" : " [v." + aimlVersion + "]"));

        return aimlParser(aimlRoot.getChildNodes());
    }

    private List<AimlCategory> aimlParser(NodeList nodes) {
        List<AimlCategory> categories = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); ++i) {
            Node node = nodes.item(i);

            String nodeName = node.getNodeName();
            switch (nodeName) {
                case AimlTag.text:
                case AimlTag.comment:
                    break;
                case AimlTag.topic:
                    categories.addAll(parseTopic(node));
                    break;
                case AimlTag.category:
                    if (!categories.add(parseCategory(node)))
                        LOG.debug(XmlHelper.node2String(node));
                    break;
                default:
                    LOG.warn("Wrong structure: <aiml> tag contain " + nodeName + " tag");
            }
        }
        return categories;
    }

    private List<AimlCategory> parseTopic(Node node) {
        List<AimlCategory> categories = new ArrayList<>();
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            String childNodeName = childNodes.item(i).getNodeName();
            switch (childNodeName) {
                case AimlTag.text:
                case AimlTag.comment:
                    break;
                case AimlTag.category:
                    categories.add(parseCategory(childNodes.item(i), getAttribute(node, AimlTag.name)));
                    break;
                default:
                    LOG.warn("Wrong structure: <topic> tag contain " + childNodeName + " tag");
            }
        }
        return categories;
    }

    private String getAttribute(Node node, String attributeName) {
        return node.getAttributes().getNamedItem(attributeName).getNodeValue();
    }

    private AimlCategory parseCategory(Node node) {
        return parseCategory(node, AimlConst.default_topic);
    }

    private AimlCategory parseCategory(Node node, String topic) {
        AimlCategory category = new AimlCategory();
        category.setTopic(topic);
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            String childNodeName = childNodes.item(i).getNodeName();
            switch (childNodeName) {
                case AimlTag.text:
                case AimlTag.comment:
                    break;
                case AimlTag.pattern:
                    category.setPattern(XmlHelper.node2String(childNodes.item(i)));
                    break;
                case AimlTag.template:
                    category.setTemplate(childNodes.item(i));
                    break;
                case AimlTag.topic:
                    category.setTopic(XmlHelper.node2String(childNodes.item(i)));
                    break;
                case AimlTag.that:
                    category.setThat(XmlHelper.node2String(childNodes.item(i)));
                    break;
                default:
                    LOG.warn("Wrong structure: <category> tag contain " + childNodeName + " tag");
            }
        }
        return category;
    }
}

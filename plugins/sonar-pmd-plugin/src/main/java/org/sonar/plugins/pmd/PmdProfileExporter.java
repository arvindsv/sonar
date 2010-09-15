/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.pmd;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.sonar.api.CoreProperties;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.pmd.xml.PmdProperty;
import org.sonar.plugins.pmd.xml.PmdRule;
import org.sonar.plugins.pmd.xml.PmdRuleset;

import com.thoughtworks.xstream.XStream;

public class PmdProfileExporter extends ProfileExporter {

  public PmdProfileExporter() {
    super(PmdConstants.REPOSITORY_KEY, PmdConstants.PLUGIN_NAME);
    setSupportedLanguages(Java.KEY);
    setMimeType("application/xml");
  }

  @Override
  public void exportProfile(RulesProfile profile, Writer writer) {
    try {
      PmdRuleset tree = createPmdRuleset(profile.getActiveRulesByRepository(PmdConstants.REPOSITORY_KEY), profile.getName());
      String xmlModules = exportPmdRulesetToXml(tree);
      writer.append(xmlModules);
    } catch (IOException e) {
      throw new SonarException("Fail to export the profile " + profile, e);
    }
  }

  protected PmdRuleset createPmdRuleset(List<ActiveRule> activeRules, String profileName) {
    PmdRuleset ruleset = new PmdRuleset(profileName);
    for (ActiveRule activeRule : activeRules) {
      if (activeRule.getRule().getPluginName().equals(CoreProperties.PMD_PLUGIN)) {
        String configKey = activeRule.getRule().getConfigKey();
        PmdRule rule = new PmdRule(configKey, PmdLevelUtils.toLevel(activeRule.getPriority()));
        List<PmdProperty> properties = null;
        if (activeRule.getActiveRuleParams() != null && !activeRule.getActiveRuleParams().isEmpty()) {
          properties = new ArrayList<PmdProperty>();
          for (ActiveRuleParam activeRuleParam : activeRule.getActiveRuleParams()) {
            properties.add(new PmdProperty(activeRuleParam.getRuleParam().getKey(), activeRuleParam.getValue()));
          }
        }
        rule.setProperties(properties);
        ruleset.addRule(rule);
        processXPathRule(activeRule.getRuleKey(), rule);
      }
    }
    return ruleset;
  }

  protected void processXPathRule(String sonarRuleKey, PmdRule rule) {
    if (PmdConstants.XPATH_CLASS.equals(rule.getRef())) {
      rule.setRef(null);
      rule.setMessage(rule.getProperty(PmdConstants.XPATH_MESSAGE_PARAM).getValue());
      rule.removeProperty(PmdConstants.XPATH_MESSAGE_PARAM);
      PmdProperty xpathExp = rule.getProperty(PmdConstants.XPATH_EXPRESSION_PARAM);
      xpathExp.setValue("" + xpathExp.getValue() + "");
      rule.setClazz(PmdConstants.XPATH_CLASS);
      rule.setName(sonarRuleKey);
    }
  }

  protected String exportPmdRulesetToXml(PmdRuleset tree) {
    XStream xstream = new XStream();
    xstream.setClassLoader(getClass().getClassLoader());
    xstream.processAnnotations(PmdRuleset.class);
    xstream.processAnnotations(PmdRule.class);
    xstream.processAnnotations(PmdProperty.class);
    return xstream.toXML(tree);
  }
}
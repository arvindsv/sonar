/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.api.rules;

import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;

import java.util.List;

/**
 * @deprecated since 2.3
 */
@Deprecated
public interface RulesRepository<LANG extends Language> extends BatchExtension, ServerExtension {

  /**
   * @return the language the repository is associated
   */
  LANG getLanguage();

  /**
   * @return the list of rules of the repository
   */
  List<Rule> getInitialReferential();

  /**
   * The method to parse the base referential of rules and return a list of rules
   *
   * @param fileContent the initial referential
   * @return a list of rules
   */
  List<Rule> parseReferential(String fileContent);

  /**
   * @return a list of profiles that are provided with the referential
   */
  List<RulesProfile> getProvidedProfiles();

}

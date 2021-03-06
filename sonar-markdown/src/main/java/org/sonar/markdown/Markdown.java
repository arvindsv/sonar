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
package org.sonar.markdown;

import org.sonar.channel.Channel;
import org.sonar.channel.ChannelDispatcher;
import org.sonar.channel.CodeReader;

import java.util.Arrays;
import java.util.List;

/**
 * Entry point of the Markdown library
 */
public final class Markdown {

  private ChannelDispatcher<MarkdownOutput> dispatcher;

  private Markdown() {
    List<Channel> markdownChannels = Arrays.<Channel>asList(
        new HtmlUrlChannel(),
        new HtmlEndOfLineChannel(),
        new HtmlEmphasisChannel(),
        new HtmlListChannel(),
        new HtmlCodeChannel(),
        new IdentifierAndNumberChannel(),
        new BlackholeChannel());
    dispatcher = new ChannelDispatcher<MarkdownOutput>(markdownChannels);
  }

  private String convert(String input) {
    CodeReader reader = new CodeReader(input);
    try {
      MarkdownOutput output = new MarkdownOutput();
      dispatcher.consume(reader, output);
      return output.toString();
      
    } finally {
      reader.close();
    }
  }

  public static String convertToHtml(String input) {
    return new Markdown().convert(input);
  }
}

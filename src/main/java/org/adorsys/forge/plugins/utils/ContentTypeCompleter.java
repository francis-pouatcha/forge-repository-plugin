package org.adorsys.forge.plugins.utils;

import java.util.Arrays;

import javax.ws.rs.core.MediaType;

import org.jboss.forge.shell.completer.SimpleTokenCompleter;

public class ContentTypeCompleter extends SimpleTokenCompleter
{

   @Override
   public Iterable<?> getCompletionTokens()
   {
      return Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);
   }

}

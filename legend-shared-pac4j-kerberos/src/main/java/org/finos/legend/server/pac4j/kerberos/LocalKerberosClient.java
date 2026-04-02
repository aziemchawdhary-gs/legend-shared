// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.server.pac4j.kerberos;

import java.util.Optional;
import org.pac4j.core.client.DirectClient;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.credentials.extractor.CredentialsExtractor;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.profile.creator.ProfileCreator;

public class LocalKerberosClient extends DirectClient
{
  private static final String X_FORWARDED_FOR_HEADER = "x-forwarded-for";

  private static String getRealRemoteAddress(WebContext context)
  {
    Optional<String> address = context.getRequestHeader(X_FORWARDED_FOR_HEADER);
    return address.orElse(context.getRemoteAddr());
  }

  @Override
  protected void internalInit(boolean forceReinit)
  {
    setAuthenticator(new Authenticator()
    {
      @Override
      public void validate(Credentials credentials, WebContext context, SessionStore sessionStore)
      {
        JEEContext jeeContext = (JEEContext) context;
        jakarta.servlet.http.HttpServletRequest request = jeeContext.getNativeRequest();
        if (!request.getLocalAddr().equals(getRealRemoteAddress(context)))
        {
          throw new CredentialsException("LocalKerberosClient only works with local requests");
        }
      }
    });
    setCredentialsExtractor(new CredentialsExtractor()
    {
      @Override
      public Optional<Credentials> extract(WebContext context, SessionStore sessionStore)
      {
        return Optional.of(LocalCredentials.INSTANCE);
      }
    });
    setProfileCreator(new ProfileCreator()
    {
      @Override
      public Optional<org.pac4j.core.profile.UserProfile> create(Credentials credentials, WebContext context, SessionStore sessionStore)
      {
        return Optional.of(new KerberosProfile((LocalCredentials) credentials));
      }
    });
  }
}

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

package org.finos.legend.server.pac4j;

import java.util.List;
import org.pac4j.core.client.Client;

/**
 * Utility class that checks whether profile storage in session is needed
 * based on the {@link SerializableProfile} annotation on client classes.
 *
 * <p>In pac4j 5.x, the {@code ProfileStorageDecision} interface was removed.
 * This class preserves the original logic as a static utility method that
 * can be used to determine the value for
 * {@link org.pac4j.core.engine.DefaultSecurityLogic#setLoadProfilesFromSession(boolean)}.
 */
public class LegendUserProfileStorageDecision
{

  public LegendUserProfileStorageDecision()
  {
  }

  /**
   * Check if any of the given clients have the {@link SerializableProfile} annotation,
   * indicating that profiles should be stored in / loaded from the session.
   *
   * @param currentClients the list of clients to check
   * @return true if at least one client has the SerializableProfile annotation
   */
  public static boolean shouldLoadProfilesFromSession(List<Client> currentClients)
  {
    if (currentClients == null || currentClients.isEmpty())
    {
      return false;
    }
    return checkForSerializableAnnotation(currentClients);
  }

  public static boolean checkForSerializableAnnotation(List<Client> currentClients)
  {
    Client c = currentClients.iterator().next();
    return c.getClass().isAnnotationPresent(SerializableProfile.class);
  }
}

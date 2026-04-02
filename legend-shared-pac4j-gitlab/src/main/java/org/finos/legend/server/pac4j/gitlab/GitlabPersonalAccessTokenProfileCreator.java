// Copyright 2021 Goldman Sachs
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

package org.finos.legend.server.pac4j.gitlab;

import java.util.Optional;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.profile.creator.ProfileCreator;

public class GitlabPersonalAccessTokenProfileCreator implements ProfileCreator
{
    private final String gitlabHost;

    public GitlabPersonalAccessTokenProfileCreator(String gitlabHost)
    {
        this.gitlabHost = gitlabHost;
    }

    @Override
    public Optional<UserProfile> create(Credentials creds, WebContext context, SessionStore sessionStore)
    {
        GitlabPersonalAccessTokenCredentials credentials = (GitlabPersonalAccessTokenCredentials) creds;
        return Optional.of(new GitlabPersonalAccessTokenProfile(credentials.getPersonalAccessToken(), credentials.getUserId(), credentials.getUserName(), this.gitlabHost));
    }
}

// Copyright 2025 Goldman Sachs
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

package org.finos.legend.server.shared.logs;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.apache.log4j.LogManager;
import org.finos.legend.server.shared.ServerIntegrationTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(DropwizardExtensionsSupport.class)
public class LogRoutingTest
{
    public static final DropwizardAppExtension<Configuration> RULE = new DropwizardAppExtension<>(TestServer.class, ResourceHelpers.resourceFilePath("testConfig.json"));

    public static final String LOG_LINE = "test method called";
    private ListAppender<ILoggingEvent> logbackAppender;

    @BeforeEach
    public void setUp()
    {
        Logger logbackLogger = (Logger)LoggerFactory.getLogger(TestServer.TestResource.class);
        logbackAppender = new ListAppender<>();
        logbackAppender.start();
        logbackLogger.addAppender(logbackAppender);
    }

    @AfterEach
    public void tearDown()
    {
        logbackAppender.stop();
    }



    @Test
    public void testDropWizardRoutingLog4jLogsToLogback()
    {

        Client client = RULE.client();
        client.target(String.format("http://localhost:%d/test", RULE.getLocalPort())).request().get();

        List<ILoggingEvent> logList = logbackAppender.list;
        assertTrue(logList.stream().anyMatch(iLoggingEvent -> iLoggingEvent.getFormattedMessage().contains(LOG_LINE)));
    }

    public static class TestServer extends Application<Configuration>
    {

        @Override
        public void run(Configuration configuration, Environment environment)
        {
            environment.jersey().register(new TestResource());
        }

        public static void main(String[] args) throws Exception
        {
            URI configUri = ServerIntegrationTestUtil.class.getResource("/testConfig.json").toURI();

            new TestServer().run("server", Paths.get(configUri).toFile().getAbsolutePath());

        }

        @jakarta.ws.rs.Path("/test")
        public static class TestResource {
            org.apache.log4j.Logger LOG4j_LOGGER = LogManager.getLogger(TestResource.class);
            @GET
            public Response TestMethod()
            {
                LOG4j_LOGGER.info(LOG_LINE);
                return Response.ok().build();
            }
        }
    }

}

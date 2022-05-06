/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.io.cdap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.plugin.hubspot.common.BaseHubspotConfig;
import io.cdap.plugin.hubspot.common.SourceHubspotConfig;
import io.cdap.plugin.hubspot.sink.batch.HubspotBatchSink;
import io.cdap.plugin.hubspot.sink.batch.HubspotOutputFormat;
import io.cdap.plugin.hubspot.sink.batch.SinkHubspotConfig;
import io.cdap.plugin.hubspot.source.batch.HubspotBatchSource;
import io.cdap.plugin.hubspot.source.batch.HubspotInputFormat;
import io.cdap.plugin.hubspot.source.batch.HubspotInputFormatProvider;
import io.cdap.plugin.salesforce.plugin.source.batch.SalesforceBatchSource;
import io.cdap.plugin.salesforce.plugin.source.batch.SalesforceSourceConfig;
import io.cdap.plugin.servicenow.source.ServiceNowInputFormat;
import io.cdap.plugin.servicenow.source.ServiceNowSource;
import io.cdap.plugin.servicenow.source.ServiceNowSourceConfig;
import io.cdap.plugin.zendesk.source.batch.ZendeskBatchSource;
import io.cdap.plugin.zendesk.source.batch.ZendeskBatchSourceConfig;
import io.cdap.plugin.zendesk.source.batch.ZendeskInputFormat;
import io.cdap.plugin.zendesk.source.batch.util.ZendeskBatchSourceConstants;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.NullableCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.cdap.context.BatchSinkContextImpl;
import org.apache.beam.sdk.io.cdap.context.BatchSourceContextImpl;
import org.apache.beam.sdk.io.hadoop.WritableCoder;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test class for {@link CdapIO}. */
@SuppressWarnings("ModifiedButNotUsed")
@RunWith(JUnit4.class)
public class CdapIOTest {

  @Rule public final transient TestPipeline p = TestPipeline.create();

  private static final Logger LOG = LoggerFactory.getLogger(CdapIOTest.class);
  private static final Gson GSON = new GsonBuilder().create();

  private static final String TEST_LOCKS_DIR_PATH = "src/test/resources";

  private static final ImmutableMap<String, Object> TEST_SALESFORCE_PARAMS_MAP =
      ImmutableMap.<String, Object>builder()
          .put("sObjectName", "sObject")
          .put("datetimeAfter", "datetime")
          .put("consumerKey", "key")
          .put("consumerSecret", "secret")
          .put("username", "user")
          .put("password", "password")
          .put("loginUrl", "https://www.google.com")
          .put("referenceName", "oldReference")
          .build();

  private static final ImmutableMap<String, Object> TEST_ZENDESK_PARAMS_MAP =
      ImmutableMap.<String, Object>builder()
          .put("referenceName", "referenceName")
          .put("adminEmail", System.getenv("ZENDESK_ADMIN_EMAIL"))
          .put("apiToken", System.getenv("ZENDESK_TOKEN"))
          .put("zendeskBaseUrl", System.getenv("ZENDESK_BASE_URL"))
          .put("subdomains", "api/v2")
          .put("maxRetryCount", 10000)
          .put("maxRetryWait", 10000)
          .put("maxRetryJitterWait", 10000)
          .put("connectTimeout", 10)
          .put("readTimeout", 10)
          .put("objectsToPull", "Groups")
          .build();

  private static final ImmutableMap<String, Object> TEST_SERVICE_NOW_PARAMS_MAP =
      ImmutableMap.<String, Object>builder()
          .put("referenceName", "referenceName")
          .put("queryMode", "Table")
          .put("tableName", "x_817050_testapp_testlabel")
          .put("clientId", System.getenv("SERVICE_NOW_CLIENT_ID"))
          .put("clientSecret", System.getenv("SERVICE_NOW_CLIENT_SECRET"))
          .put("user", System.getenv("SERVICE_NOW_USER"))
          .put("password", System.getenv("SERVICE_NOW_PASSWORD"))
          .put("restApiEndpoint", System.getenv("SERVICE_NOW_API_ENDPOINT"))
          .put("valueType", "Actual")
          .build();
  private static final long NUM_OF_TEST_SERVICE_NOW_RECORDS =
      Long.parseLong(System.getenv("NUM_OF_TEST_SERVICE_NOW_RECORDS"));
  private static final String SERVICE_NOW_TEST_RECORDS_PREFIX =
      System.getenv("SERVICE_NOW_TEST_RECORDS_PREFIX");

  private static final long NUM_OF_TEST_HUBSPOT_CONTACTS =
      Long.parseLong(System.getenv("HUBSPOT_CONTACTS_NUM"));
  private static final ImmutableMap<String, Object> TEST_HUBSPOT_PARAMS_MAP =
      ImmutableMap.<String, Object>builder()
          .put("apiServerUrl", BaseHubspotConfig.DEFAULT_API_SERVER_URL)
          .put("objectType", "Contacts")
          .put("referenceName", "Contacts")
          .put("apiKey", System.getenv("HUBSPOT_TOKEN"))
          .build();

  private static final ImmutableMap<String, Object> TEST_HUBSPOT_COMPANIES_PARAMS_MAP =
      ImmutableMap.<String, Object>builder()
          .put("apiServerUrl", BaseHubspotConfig.DEFAULT_API_SERVER_URL)
          .put("objectType", "Companies")
          .put("referenceName", "COmpanies")
          .put("apiKey", System.getenv("HUBSPOT_TOKEN"))
          .build();

  private static final String TEST_HUBSPOT_COMPANY =
      "{\n"
          + "  \"properties\": [\n"
          + "    {\n"
          + "      \"name\": \"name\",\n"
          + "      \"value\": \"A company name\"\n"
          + "    },\n"
          + "    {\n"
          + "      \"name\": \"description\",\n"
          + "      \"value\": \"A company description\"\n"
          + "    }\n"
          + "  ]\n"
          + "}";

  public static class JsonElementCoder extends CustomCoder<JsonElement> {
    private static final JsonElementCoder CODER = new JsonElementCoder();
    private static final StringUtf8Coder STRING_CODER = StringUtf8Coder.of();

    public static JsonElementCoder of() {
      return CODER;
    }

    @Override
    public void encode(JsonElement value, OutputStream outStream) throws IOException {
      STRING_CODER.encode(value.toString(), outStream);
    }

    @Override
    public JsonElement decode(InputStream inStream) throws IOException {
      return JsonParser.parseString(STRING_CODER.decode(inStream));
    }
  }

  @Test
  public void testReadFromZendesk() {

    ZendeskBatchSourceConfig pluginConfig =
        new ConfigWrapper<>(ZendeskBatchSourceConfig.class)
            .withParams(TEST_ZENDESK_PARAMS_MAP)
            .build();

    CdapIO.Read<NullWritable, StructuredRecord> reader =
        CdapIO.<NullWritable, StructuredRecord>read()
            .withCdapPluginClass(ZendeskBatchSource.class)
            .withPluginConfig(pluginConfig)
            .withKeyClass(NullWritable.class)
            .withValueClass(StructuredRecord.class);

    assertNotNull(reader.getPluginConfig());
    assertNotNull(reader.getCdapPlugin());
    assertFalse(reader.getCdapPlugin().isUnbounded());
    assertEquals(BatchSourceContextImpl.class, reader.getCdapPlugin().getContext().getClass());

    PCollection<KV<NullWritable, StructuredRecord>> input =
        p.apply(reader)
            .setCoder(
                KvCoder.of(
                    NullableCoder.of(WritableCoder.of(NullWritable.class)),
                    SerializableCoder.of(StructuredRecord.class)));

    PAssert.that(input)
        .satisfies(
            (iterable) -> {
              List<KV<NullWritable, StructuredRecord>> list =
                  (List<KV<NullWritable, StructuredRecord>>) iterable;
              assertEquals(1, list.size());
              StructuredRecord record = list.get(0).getValue();
              assertNotNull(record);
              assertEquals(TEST_ZENDESK_PARAMS_MAP.get("objectsToPull"), record.get("object"));
              return null;
            });
    p.run();

    assertEquals(ZendeskInputFormat.class, reader.getCdapPlugin().getFormatClass());

    Configuration hadoopConf = reader.getCdapPlugin().getHadoopConfiguration();
    String configJson = hadoopConf.get(ZendeskBatchSourceConstants.PROPERTY_CONFIG_JSON);
    ZendeskBatchSourceConfig configFromJson =
        GSON.fromJson(configJson, ZendeskBatchSourceConfig.class);

    assertEquals(pluginConfig.getAdminEmail(), configFromJson.getAdminEmail());
  }

  @Test
  public void testReadFromServiceNow() {

    ServiceNowSourceConfig pluginConfig =
        new ConfigWrapper<>(ServiceNowSourceConfig.class)
            .withParams(TEST_SERVICE_NOW_PARAMS_MAP)
            .build();

    CdapIO.Read<NullWritable, StructuredRecord> reader =
        CdapIO.<NullWritable, StructuredRecord>read()
            .withCdapPluginClass(ServiceNowSource.class)
            .withPluginConfig(pluginConfig)
            .withKeyClass(NullWritable.class)
            .withValueClass(StructuredRecord.class);

    assertNotNull(reader.getPluginConfig());
    assertNotNull(reader.getCdapPlugin());
    assertFalse(reader.getCdapPlugin().isUnbounded());
    assertEquals(BatchSourceContextImpl.class, reader.getCdapPlugin().getContext().getClass());

    PCollection<KV<NullWritable, StructuredRecord>> input =
        p.apply(reader)
            .setCoder(
                KvCoder.of(
                    NullableCoder.of(WritableCoder.of(NullWritable.class)),
                    SerializableCoder.of(StructuredRecord.class)));

    PAssert.that(input)
        .satisfies(
            (iterable) -> {
              List<KV<NullWritable, StructuredRecord>> list =
                  (List<KV<NullWritable, StructuredRecord>>) iterable;
              assertEquals(NUM_OF_TEST_SERVICE_NOW_RECORDS, list.size());
              for (int i = 0; i < list.size(); i++) {
                StructuredRecord record = list.get(0).getValue();
                assertNotNull(record);
                String recordName = record.get("name");
                assertNotNull(recordName);
                assertTrue(recordName.startsWith(SERVICE_NOW_TEST_RECORDS_PREFIX));
              }
              return null;
            });
    p.run();

    assertEquals(ServiceNowInputFormat.class, reader.getCdapPlugin().getFormatClass());
  }

  @Test
  public void testReadFromHubspotBatch() {

    SourceHubspotConfig pluginConfig =
        new ConfigWrapper<>(SourceHubspotConfig.class).withParams(TEST_HUBSPOT_PARAMS_MAP).build();

    CdapIO.Read<NullWritable, JsonElement> reader =
        CdapIO.<NullWritable, JsonElement>read()
            .withCdapPluginClass(HubspotBatchSource.class)
            .withPluginConfig(pluginConfig)
            .withKeyClass(NullWritable.class)
            .withValueClass(JsonElement.class);

    assertNotNull(reader.getPluginConfig());
    assertNotNull(reader.getCdapPlugin());
    assertFalse(reader.getCdapPlugin().isUnbounded());
    assertEquals(BatchSourceContextImpl.class, reader.getCdapPlugin().getContext().getClass());

    p.getCoderRegistry().registerCoderForClass(JsonElement.class, JsonElementCoder.of());

    PCollection<KV<NullWritable, JsonElement>> input =
        p.apply(reader)
            .setCoder(
                KvCoder.of(
                    NullableCoder.of(WritableCoder.of(NullWritable.class)), JsonElementCoder.of()));

    PAssert.that(input)
        .satisfies(
            (map) -> {
              long numOfCorrectRecords = 0;
              for (KV<NullWritable, JsonElement> record : map) {
                assertNotNull(record.getValue());
                JsonObject contactProperties =
                    record.getValue().getAsJsonObject().getAsJsonObject("properties");
                String firstName =
                    contactProperties.getAsJsonObject("firstname").get("value").toString();
                String lastname =
                    contactProperties.getAsJsonObject("lastname").get("value").toString();
                assertFalse(StringUtils.isEmpty(firstName));
                assertFalse(StringUtils.isEmpty(lastname));
                numOfCorrectRecords++;
              }
              assertEquals(NUM_OF_TEST_HUBSPOT_CONTACTS, numOfCorrectRecords);
              return null;
            });
    p.run();

    assertEquals(HubspotInputFormat.class, reader.getCdapPlugin().getFormatClass());

    Configuration hadoopConf = reader.getCdapPlugin().getHadoopConfiguration();
    String configJson = hadoopConf.get(HubspotInputFormatProvider.PROPERTY_CONFIG_JSON);
    SourceHubspotConfig configFromJson = GSON.fromJson(configJson, SourceHubspotConfig.class);

    assertEquals(pluginConfig.getObjectType(), configFromJson.getObjectType());
  }

  @Test
  public void testWriteToHubspotBatch() {

    SinkHubspotConfig sinkConfig =
        new ConfigWrapper<>(SinkHubspotConfig.class)
            .withParams(TEST_HUBSPOT_COMPANIES_PARAMS_MAP)
            .build();

    CdapIO.Write<NullWritable, String> writer =
        CdapIO.<NullWritable, String>write()
            .withCdapPluginClass(HubspotBatchSink.class)
            .withPluginConfig(sinkConfig)
            .withKeyClass(NullWritable.class)
            .withValueClass(String.class)
            .withLocksDirPath(TEST_LOCKS_DIR_PATH);

    assertNotNull(writer.getPluginConfig());
    assertNotNull(writer.getCdapPlugin());
    assertFalse(writer.getCdapPlugin().isUnbounded());
    assertEquals(BatchSinkContextImpl.class, writer.getCdapPlugin().getContext().getClass());

    p.apply(
            Create.of(KV.of(NullWritable.get(), TEST_HUBSPOT_COMPANY))
                .withCoder(
                    KvCoder.of(
                        NullableCoder.of(WritableCoder.of(NullWritable.class)),
                        StringUtf8Coder.of())))
        .setTypeDescriptor(
            TypeDescriptors.kvs(
                new TypeDescriptor<NullWritable>() {}, new TypeDescriptor<String>() {}))
        .apply(writer);

    p.run();

    assertEquals(HubspotOutputFormat.class, writer.getCdapPlugin().getFormatClass());

    Configuration hadoopConf = writer.getCdapPlugin().getHadoopConfiguration();
    String configJson = hadoopConf.get("cdap.Hubspot.sink.config");
    SinkHubspotConfig configFromJson = GSON.fromJson(configJson, SinkHubspotConfig.class);

    assertEquals(sinkConfig.getObjectType(), configFromJson.getObjectType());
  }

  @Test
  public void testReadFromSalesforce() {
    try {

      SalesforceSourceConfig pluginConfig =
          new ConfigWrapper<>(SalesforceSourceConfig.class)
              .withParams(TEST_SALESFORCE_PARAMS_MAP)
              .build();

      CdapIO.Read<Schema, Map<String, String>> reader =
          CdapIO.<Schema, Map<String, String>>read()
              .withCdapPluginClass(SalesforceBatchSource.class)
              .withPluginConfig(pluginConfig);

      assertNotNull(reader.getPluginConfig());
      assertNotNull(reader.getCdapPlugin());
      assertFalse(reader.getCdapPlugin().isUnbounded());
      assertEquals(BatchSourceContextImpl.class, reader.getCdapPlugin().getContext().getClass());

      // TODO: Provide params needed for Salesforce connection, run test pipeline
      //  and add assertions for Hadoop conf and pipeline result

    } catch (Exception e) {
      LOG.error("Error occurred", e);
      fail();
    }
  }
}
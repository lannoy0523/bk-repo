package com.tencent.bkrepo.composer.util

import com.tencent.bkrepo.composer.pojo.ComposerMetadata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class DecompressUtilTest {
    @Test
    fun jsonTest() {
        val json = "{\n" +
            "    \"name\": \"monolog/monolog\",\n" +
            "    \"description\": \"Sends your logs to files, sockets, inboxes, databases and various web services\",\n" +
            "    \"keywords\": [\"log\", \"logging\", \"psr-3\"],\n" +
            "    \"homepage\": \"http://github.com/Seldaek/monolog\",\n" +
            "    \"type\": \"library\",\n" +
            "    \"license\": \"MIT\",\n" +
            "    \"authors\": [\n" +
            "        {\n" +
            "            \"name\": \"Jordi Boggiano\",\n" +
            "            \"email\": \"j.boggiano@seld.be\",\n" +
            "            \"homepage\": \"http://seld.be\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"version\": \"2.1.0\",\n" +
            "    \"require\": {\n" +
            "        \"php\": \"^7.2\",\n" +
            "        \"psr/log\": \"^1.0.1\"\n" +
            "    },\n" +
            "    \"require-dev\": {\n" +
            "        \"aws/aws-sdk-php\": \"^2.4.9 || ^3.0\",\n" +
            "        \"doctrine/couchdb\": \"~1.0@dev\",\n" +
            "        \"elasticsearch/elasticsearch\": \"^6.0\",\n" +
            "        \"graylog2/gelf-php\": \"^1.4.2\",\n" +
            "        \"jakub-onderka/php-parallel-lint\": \"^0.9\",\n" +
            "        \"php-amqplib/php-amqplib\": \"~2.4\",\n" +
            "        \"php-console/php-console\": \"^3.1.3\",\n" +
            "        \"phpspec/prophecy\": \"^1.6.1\",\n" +
            "        \"phpunit/phpunit\": \"^8.3\",\n" +
            "        \"predis/predis\": \"^1.1\",\n" +
            "        \"rollbar/rollbar\": \"^1.3\",\n" +
            "        \"ruflin/elastica\": \">=0.90 <3.0\",\n" +
            "        \"swiftmailer/swiftmailer\": \"^5.3|^6.0\"\n" +
            "    },\n" +
            "    \"suggest\": {\n" +
            "        \"graylog2/gelf-php\": \"Allow sending log messages to a GrayLog2 server\",\n" +
            "        \"doctrine/couchdb\": \"Allow sending log messages to a CouchDB server\",\n" +
            "        \"ruflin/elastica\": \"Allow sending log messages to an Elastic Search server\",\n" +
            "        \"elasticsearch/elasticsearch\": \"Allow sending log messages to an Elasticsearch server via official client\",\n" +
            "        \"php-amqplib/php-amqplib\": \"Allow sending log messages to an AMQP server using php-amqplib\",\n" +
            "        \"ext-amqp\": \"Allow sending log messages to an AMQP server (1.0+ required)\",\n" +
            "        \"ext-mongodb\": \"Allow sending log messages to a MongoDB server (via driver)\",\n" +
            "        \"mongodb/mongodb\": \"Allow sending log messages to a MongoDB server (via library)\",\n" +
            "        \"aws/aws-sdk-php\": \"Allow sending log messages to AWS services like DynamoDB\",\n" +
            "        \"rollbar/rollbar\": \"Allow sending log messages to Rollbar\",\n" +
            "        \"php-console/php-console\": \"Allow sending log messages to Google Chrome\",\n" +
            "        \"ext-mbstring\": \"Allow to work properly with unicode symbols\"\n" +
            "    },\n" +
            "    \"autoload\": {\n" +
            "        \"psr-4\": {\"Monolog\\\\\": \"src/Monolog\"}\n" +
            "    },\n" +
            "    \"autoload-dev\": {\n" +
            "        \"psr-4\": {\"Monolog\\\\\": \"tests/Monolog\"}\n" +
            "    },\n" +
            "    \"provide\": {\n" +
            "        \"psr/log-implementation\": \"1.0.0\"\n" +
            "    },\n" +
            "    \"extra\": {\n" +
            "        \"branch-alias\": {\n" +
            "            \"dev-master\": \"2.x-dev\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"scripts\": {\n" +
            "        \"test\": [\n" +
            "            \"parallel-lint . --exclude vendor\",\n" +
            "            \"phpunit\"\n" +
            "        ]\n" +
            "    },\n" +
            "    \"config\": {\n" +
            "        \"sort-packages\": true\n" +
            "    }\n" +
            "}\n"

        val json01 = "{\n" +
            "    \"name\": \"weaving/blog\",\n" +
            "    \"description\": \"It is a test project!\",\n" +
            "    \"type\": \"library\",\n" +
            "    \"version\": \"1.0\",\n" +
            "    \"license\": \"MIT\",\n" +
            "    \"authors\": [\n" +
            "        {\n" +
            "            \"name\": \"weaving\",\n" +
            "            \"email\": \"onnt1997@outlook.com\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"minimum-stability\": \"dev\"\n" +
            "}"
        val composerMetadata = JsonUtil.mapper.readValue<ComposerMetadata>(json, ComposerMetadata::class.java)
        val composerMetadata01 = JsonUtil.mapper.readValue<ComposerMetadata>(json01, ComposerMetadata::class.java)
        Assertions.assertNotNull(composerMetadata)
        Assertions.assertNotNull(composerMetadata01)
    }
}

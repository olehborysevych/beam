# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Module contains the client to communicate with Google Cloud Datastore
"""
import string
import uuid
from datetime import datetime
from typing import List

from google.cloud import datastore
from tqdm import tqdm

from config import Config
from helper import Example

import constant
from hashlib import sha256
from base64 import urlsafe_b64encode


# https://cloud.google.com/datastore/docs/concepts/entities
class DatastoreClient:
    """DatastoreClient is a datastore client for sending a request to the Google."""

    def __init__(self):
        self._datastore_client = datastore.Client(
            namespace=constant.NAMESPACE,
            project=Config.GOOGLE_CLOUD_PROJECT
        )

    def save_to_cloud_datastore(self, examples: List[Example]):
        """
        Save examples, output and meta to datastore

        Args:
            examples: precompiled examples
        """

        snippets = []
        now = datetime.today()
        last_schema_version_query = self._datastore_client.query(kind=constant.SCHEMA_KIND)
        schema_keys = last_schema_version_query.fetch()
        with self._datastore_client.transaction():
            for example in tqdm(examples):
                snippet_id = uuid.UUID  # The grpc client will call the router to generate an ID
                snippet_entity = datastore.Entity(self._get_key(constant.SNIPPET_KIND, str(snippet_id)))
                snippet_entity.update(
                    {
                        "sdk": self._get_key(constant.SDK_KIND, example.sdk),
                        "created": now,
                        "lVisited": now,
                        "origin": "PG_EXAMPLES",
                        "numberOfFiles": 1,
                        "schVer": self._get_key(constant.SCHEMA_KIND, "version")
                    }
                )
                snippets.append(snippet_entity)
            self._datastore_client.put_multi(snippets)

    def _generate_id(self, salt, content: string, length: int) -> string:
        hash_init = sha256()
        hash_init.update(salt + content)
        return urlsafe_b64encode(hash_init.digest())[:length]

    def _get_key(self, kind, identifier: str) -> datastore.key:
        return self._datastore_client.key(kind, identifier)

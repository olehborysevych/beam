// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package migration

import (
	"beam.apache.org/playground/backend/internal/db/datastore"
	"beam.apache.org/playground/backend/internal/db/schema"
	"beam.apache.org/playground/backend/internal/environment"
	"context"
	"os"
	"testing"
)

const (
	datastoreEmulatorHostKey   = "DATASTORE_EMULATOR_HOST"
	datastoreEmulatorHostValue = "127.0.0.1:8888"
	datastoreEmulatorProjectId = "test"
)

var datastoreDb *datastore.Datastore
var ctx context.Context

func TestMain(m *testing.M) {
	setup()
	code := m.Run()
	teardown()
	os.Exit(code)
}

func setup() {
	datastoreEmulatorHost := os.Getenv(datastoreEmulatorHostKey)
	if datastoreEmulatorHost == "" {
		if err := os.Setenv(datastoreEmulatorHostKey, datastoreEmulatorHostValue); err != nil {
			panic(err)
		}
	}
	ctx = context.Background()
	var err error
	datastoreDb, err = datastore.New(ctx, datastoreEmulatorProjectId)
	if err != nil {
		panic(err)
	}
}

func teardown() {
	if err := datastoreDb.Client.Close(); err != nil {
		panic(err)
	}
}

func TestInitialStructure_InitiateData(t *testing.T) {
	tests := []struct {
		name    string
		dbArgs  *schema.DBArgs
		wantErr bool
	}{
		{
			name: "Test migration with version 0.0.1 in the usual case",
			dbArgs: &schema.DBArgs{
				Ctx:    ctx,
				Db:     datastoreDb,
				AppEnv: environment.NewApplicationEnvs("/app", "", "", "", "", "MOCK_SALT", "", "", "../../../../../sdks.yaml", nil, 0, "", 0, 11),
			},
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			is := new(InitialStructure)
			err := is.InitiateData(tt.dbArgs)
			if (err != nil) != tt.wantErr {
				t.Errorf("InitiateData() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
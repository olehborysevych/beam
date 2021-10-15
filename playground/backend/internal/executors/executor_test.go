package executors

import (
	"beam.apache.org/playground/backend/internal/environment"
	"beam.apache.org/playground/backend/internal/validators"
	"io/fs"
	"os"
	"os/exec"
	"reflect"
	"testing"
)

//var (
//	pipelineId = uuid.New()
//	env        *environment.Environment
//	lc         *fs_tool.LifeCycle
//	filePath   string
//)

const (
	//	javaCode   = "class HelloWorld {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World!\");\n    }\n}"
	javaConfig = "{\n  \"compile_cmd\": \"javac\",\n  \"run_cmd\": \"java\",\n  \"compile_args\": [\"-d\", \"bin\", \"-classpath\"],\n  \"run_args\": [\"-cp\", \"bin:\"]\n}"
)

//
//func TestMain(m *testing.M) {
//	lc = setup()
//	defer teardown(lc)
//	m.Run()
//}
//
//func setup() *fs_tool.LifeCycle {
//	lc, _ := fs_tool.NewLifeCycle(pb.Sdk_SDK_JAVA, pipelineId)
//	_ = lc.CreateFolders()
//	filePath, _ = lc.CreateExecutableFile(javaCode)
//	p, _ := os.Getwd()
//	os.MkdirAll("configs", fs.ModePerm)
//	os.WriteFile("configs/SDK_JAVA.json", []byte(javaConfig), 0600)
//	os.Setenv("CONFIG_FOLDER", p+"/configs/")
//	env = environment.NewEnvironment()
//	return lc
//}
//
//func teardown(lc *fs_tool.LifeCycle) {
//	err := lc.DeleteFolders()
//	if err != nil {
//		return
//	}
//	err = os.RemoveAll("configs")
//	if err != nil {
//		return
//	}
//}

//func TestNewProvider(t *testing.T) {
//	validatorsFuncs := validators.GetJavaValidators(lc.GetAbsoluteExecutableFilePath())
//	type args struct {
//		envs            environment.BeamEnvs
//		workingDir      string
//		filePath        string
//		validatorsFuncs *[]validators.Validator
//	}
//	tests := []struct {
//		name string
//		args args
//		want *Executor
//	}{
//		{
//			name: "NewCmdProvider",
//			args: args{
//				envs:            env.BeamSdkEnvs,
//				workingDir:      "./",
//				filePath:        filePath,
//				validatorsFuncs: validatorsFuncs,
//			},
//			want: &Executor{
//				dirPath:        "./",
//				validators:     *validatorsFuncs,
//				compileCommand: "javac",
//				compileArgs:    []string{"-d", "bin", "-classpath", "/opt/apache/beam/jars/beam-sdks-java-harness.jar", filePath},
//				runCommand:     "java",
//				runArgs:        []string{"-cp", "bin:/opt/apache/beam/jars/beam-sdks-java-harness.jar:/opt/apache/beam/jars/beam-runners-direct.jar:/opt/apache/beam/jars/slf4j-jdk14.jar"},
//			},
//		},
//	}
//	for _, tt := range tests {
//		t.Run(tt.name, func(t *testing.T) {
//			if got := NewCmdProvider(tt.args.envs, tt.args.workingDir, filePath, tt.args.validatorsFuncs); !reflect.DeepEqual(got, tt.want) {
//				t.Errorf("NewCmdProvider() = %v, want %v", got, tt.want)
//			}
//		})
//	}
//}

func TestExecutor_Compile(t *testing.T) {
	type fields struct {
		compileArgs cmdSetting
		runArgs     cmdSetting
		validators  []validators.Validator
	}
	tests := []struct {
		name   string
		fields fields
		want   *exec.Cmd
	}{
		{
			name: "TestCompile",
			fields: fields{
				compileArgs: cmdSetting{
					fileName:    "filePath",
					dirPath:     "./",
					commandName: "javac",
					commandArgs: []string{"-d", "bin", "-classpath", "/opt/apache/beam/jars/beam-sdks-java-harness.jar"},
				},
			},
			want: &exec.Cmd{
				Path:         "/usr/bin/javac",
				Args:         []string{"javac", "-d", "bin", "-classpath", "/opt/apache/beam/jars/beam-sdks-java-harness.jar", "filePath"},
				Env:          nil,
				Dir:          "",
				Stdin:        nil,
				Stdout:       nil,
				Stderr:       nil,
				ExtraFiles:   nil,
				SysProcAttr:  nil,
				Process:      nil,
				ProcessState: nil,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ex := &Executor{
				compileArgs: tt.fields.compileArgs,
				runArgs:     tt.fields.runArgs,
				validators:  tt.fields.validators,
			}
			if got := ex.Compile(); !reflect.DeepEqual(got.String(), tt.want.String()) {
				t.Errorf("Compile() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestExecutor_Run(t *testing.T) {
	type fields struct {
		compileArgs cmdSetting
		runArgs     cmdSetting
		validators  []validators.Validator
	}
	tests := []struct {
		name   string
		fields fields
		want   *exec.Cmd
	}{
		{
			name: "TestRun",
			fields: fields{
				runArgs: cmdSetting{
					fileName:    "HelloWorld",
					dirPath:     "./",
					commandName: "java",
					commandArgs: []string{"-cp", "bin:/opt/apache/beam/jars/beam-sdks-java-harness.jar:" +
						"/opt/apache/beam/jars/beam-runners-direct.jar:/opt/apache/beam/jars/slf4j-jdk14.jar"},
				},
			},
			want: &exec.Cmd{
				Path: "/usr/bin/java",
				Args: []string{"java", "-cp", "bin:/opt/apache/beam/jars/beam-sdks-java-harness.jar:" +
					"/opt/apache/beam/jars/beam-runners-direct.jar:/opt/apache/beam/jars/slf4j-jdk14.jar", "HelloWorld"},
				Env:          nil,
				Dir:          "",
				Stdin:        nil,
				Stdout:       nil,
				Stderr:       nil,
				ExtraFiles:   nil,
				SysProcAttr:  nil,
				Process:      nil,
				ProcessState: nil,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ex := &Executor{
				compileArgs: tt.fields.compileArgs,
				runArgs:     tt.fields.runArgs,
				validators:  tt.fields.validators,
			}
			if got := ex.Run(); !reflect.DeepEqual(got.String(), tt.want.String()) {
				t.Errorf("Run() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestBaseExecutorBuilder(t *testing.T) {
	validatorsFuncs := validators.GetJavaValidators("filePath")
	p, _ := os.Getwd()
	os.MkdirAll("configs", fs.ModePerm)
	os.WriteFile("configs/SDK_JAVA.json", []byte(javaConfig), 0600)
	os.Setenv("CONFIG_FOLDER", p+"/configs/")
	env := environment.NewEnvironment()
	type args struct {
		envs            environment.BeamEnvs
		workingDir      string
		filePath        string
		validatorsFuncs *[]validators.Validator
	}
	tests := []struct {
		name string
		args args
		want Executor
	}{
		{
			name: "NewCmdProvider",
			args: args{
				envs:            env.BeamSdkEnvs,
				workingDir:      "./",
				filePath:        "filePath",
				validatorsFuncs: validatorsFuncs,
			},
			want: Executor{
				compileArgs: cmdSetting{
					fileName:    "filePath",
					dirPath:     "./",
					commandName: "javac",
					commandArgs: []string{"-d", "bin", "-classpath", "/opt/apache/beam/jars/beam-sdks-java-harness.jar"},
				},
				runArgs: cmdSetting{
					fileName:    "HelloWorld",
					dirPath:     "./",
					commandName: "java",
					commandArgs: []string{"-cp", "bin:/opt/apache/beam/jars/beam-sdks-java-harness.jar:/opt/apache/beam/jars/beam-runners-direct.jar:/opt/apache/beam/jars/slf4j-jdk14.jar"},
				},
				validators: *validatorsFuncs,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := BaseExecutorBuilder(tt.args.envs, tt.args.workingDir, tt.args.filePath, tt.args.validatorsFuncs).Build(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("BaseExecutorBuilder() = %v, want %v", got, tt.want)
			}
		})
	}
	err := os.RemoveAll("configs")
	if err != nil {
		return
	}
}

package ru.svyaznoy.jenkins1c
import ru.svyaznoy.jenkins1c.shell

class BuildSteps {

    static def sh = new shell()

   static void addShellStep(def job, def command) {
        def code = sh.getScript(command)
        job.with {
            steps { 
                batchFile(code)
            }
        }
    }

    static void addGitSource(def job, String gitUrl, String gitBranch){
        job.with{
            scm {
                git {
                    remote {
                        url(gitUrl)
                    }
                    branch(gitBranch)
                }
            }
        }
    }

    static void addStepCleanDatabase(def job){
        addShellStep(job, '''
echo Cleaning
erase /F /S *.zip
call packman clear
''')

    }
    
    static void addStepLoadSourceFromStorage(def job){
        addShellStep(job, '''
echo loading sources
call packman load-storage %storage_dir% -use-tool1cd -details storage.info
''')
    
    }

    static void addStepMakeSyntaxCheck(def job, def givenChecks){
        
        def checks
        if(givenChecks == null)
            checks = "-ThinClient -Server -ExternalConnection"
        else
            checks = givenChecks

        addShellStep(job, """
echo Syntax check
call packman check $checks
""")
    }

    static void addStepRunXUnit(def job, def testSrc, def report, def thinClient){
        
        def thinClientKey = ''
        if(thinClient == true) {
            thinClientKey = '--ordinaryapp 0'
        }
        addShellStep(job, """
set CICD_TOOLS=C:/CICD

call runner xunit \"${testSrc}\" --ibname "/F%CD%/.packman/v8r_TempDB" $thinClientKey --reportsxunit "ГенераторОтчетаJUnitXML{${report}}" --pathxunit %CICD_TOOLS%/xUnit/4.0/xddTestRunner.epf
""")
    }


    static void addStepMakeVendorCF(def job){
        addShellStep(job, '''
echo making vendor cf
call packman make-cf
''')
    }

    static void addStepBuildDistribution(def job){
        addShellStep(job, '''
echo making files
call packman make-dist build/package.edf -files storage.info
''')
    }
    
    static void addStepCreateZipArtifact(def job){
        addShellStep(job, '''
echo making zip
set VPACKMAN_BUILDVARS=НомерСборкиСервера=%BUILD_NUMBER%
call packman zip-dist -name-prefix %project_name% -out .

''')
    }

    static void addStepMigrateTestDB(def job){
        addShellStep(job, '''
echo Running Migration Task
call deployka run /F\"\\"%WORKSPACE%/.packman/v8r_TempDB\\"" -v8version %v8version% -command "MIGRATE"
''')
    }

    static void addStandardSteps(def job, def projectConfig) {
        addStepCleanDatabase(job)
        addStepLoadSourceFromStorage(job)
        addStepMakeSyntaxCheck(job, projectConfig.syntaxChecks)
        addStepMigrateTestDB(job)
        addStepRunXUnit(job, "%WORKSPACE%/build", "%WORKSPACE%/.packman/init-db.xml", projectConfig.preferThinClient)
        addStepRunXUnit(job, "./tests", "%WORKSPACE%/.packman/xunit-report.xml", projectConfig.preferThinClient)
        addStepMakeVendorCF(job)
        addStepBuildDistribution(job)
        addStepCreateZipArtifact(job)
    
    }
    
    static void addStandardPublishers(def job){
        job.with{
            publishers {
                junit {
                    testResults('.packman/*.xml')
                }
                artifactArchiver {
                    artifacts('*.zip')
                }
            }
        }
    }

    static void preBuild(def job){
        job.with{
            logRotator(20, 3)
            wrappers {
                preBuildCleanup {
                    includePattern('*.zip')
                }
            }
        }
    }

    static void setProperties(def job, def storage, def project_name, def v8version)
    {
        job.with{
            wrappers{
                environmentVariables {
                    env("storage_dir", "$storage".replace("\\", "\\\\"))
                    env("v8version", "$v8version")
                    env("project_name", "$project_name")
                }
            }
        }
    }
    

}
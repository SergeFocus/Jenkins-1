package ru.svyaznoy.jenkins1c
import ru.svyaznoy.jenkins1c.shell

class DeploySteps {

    static def sh = new shell()

    static void addShellStep(def job, def command) {
        def code = sh.getScript(command)
        job.with {
            steps { 
                batchFile(code)
            }
        }
    }

    static void setProperties(def job, def v8version, def deployTarget)
    {
        job.with{
            parameters {
                choiceParam('update_mode', ['-auto', '-cf', '-load', '-skip'], 'Режим развертывания: CF, CFU, Полная загрузка')
            }
            logRotator(-1, 3)
            wrappers{
                
                preBuildCleanup {
                    includePattern('*.zip')
                }

                environmentVariables {
                    env("v8version", "$v8version")
                    env("ras_port", deployTarget.rasPort)
                    env("db_server", deployTarget.dbServer)
                    env("db", deployTarget.db)
                }

                credentialsBinding {
                    usernamePassword('DB_USER', 'DB_PASSWORD', deployTarget.credentials)
                }

            }
        }
    }

    static void setAutoDeploy(def job, def buildName) {
        job.with {
            triggers {
                upstream(buildName, 'UNSTABLE')
            }
        }
    }

    static void addArtifactDownload(def job, def buildName) {
        job.with {
            steps {
                copyArtifacts(buildName) {
                    includePatterns('*.zip')
                    buildSelector {
                        latestSuccessful(true)
                    }
                }
            }
        }
    }

    static void addKillSessionsStep(def job) {
        addLockSessions(job)
        addKillSessions(job)
    }

    static void addLockSessions(def job) {
        addShellStep(job, '''
call deployka session lock -ras localhost:%RAS_PORT% -db %DB% -db-user %DB_USER% -db-pwd %DB_PASSWORD% -lockuccode AutoDeploy -v8version %v8version%
''')
    }
    
    static void addKillSessions(def job) {
        addShellStep(job, '''
call deployka session kill -ras localhost:%RAS_PORT% -db %DB% -db-user %DB_USER% -db-pwd %DB_PASSWORD% -lockuccode AutoDeploy -v8version %v8version%
''')
    }
    
    static void addAllowSessionsStep(def job) {
        
        def cmd = sh.getScript('call deployka session unlock -ras localhost:%RAS_PORT% -db %DB% -db-user %DB_USER% -db-pwd %DB_PASSWORD% -lockuccode AutoDeploy -v8version %v8version%')
        job.with {
            publishers {
                postBuildTask {
                    task(' ', cmd)
                }
            }
        }
    }

    static void dispatchDeploySteps(def job, def stepNames) {

        for(step in stepNames)
        {
            switch(step) {
                case 'loadcfg':
                    addLoadCfgStep(job)
                    break
                case 'dbupdate':
                    addDbUpdateStep(job)
                    break
                case ~/run:[a-zA-Z]+/:
                    addRunStep(job, step.substring(4))
                    break
                default:
                    throw new Exception("Unknopwn step '$step'")
            }
        }

    }

    static void addStandardSteps(job) {
        dispatchDeploySteps(job, ['loadcfg', 'dbupdate', 'run:MIGRATE'])
    }

    static void addLoadCfgStep(def job) {
        
        addShellStep(job, '''
set CICD_TOOLS=C:/CICD
set extractor=%CICD_TOOLS%/additional/artifact-extractor.os

oscript "%extractor%" "%WORKSPACE%" "%WORKSPACE%/DIST"
IF %ERRORLEVEL% == 1 exit /B 1

echo calling deployka
call deployka loadcfg /S"%DB_SERVER%\\%DB%" "%WORKSPACE%/DIST" /mode %update_mode% -db-user %DB_USER% -db-pwd %DB_PASSWORD% -uccode AutoDeploy -v8version %v8version%
''')

    }

    static void addDbUpdateStep(def job) {
        addShellStep(job, '''
call deployka dbupdate /S"%DB_SERVER%\\%DB%" -db-user %DB_USER% -db-pwd %DB_PASSWORD% -uccode AutoDeploy -v8version %v8version%
''')
    }

    static void addRunStep(def job, def command) {
        def cmd = 'call deployka run /S"%DB_SERVER%\\%DB%" -v8version %v8version% -command '+command+' -db-user %DB_USER% -db-pwd %DB_PASSWORD% -uccode AutoDeploy -v8version %v8version%'
        addShellStep(job, cmd)
    }
    
    
}
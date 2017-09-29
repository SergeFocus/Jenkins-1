package ru.svyaznoy.jenkins1c
import ru.svyaznoy.jenkins1c.shell

class GitSync {
    
    static def sh = new shell()

    static void addShellStep(def job, def command) {
        def code = sh.getScript(command)
        job.with {
            steps { 
                batchFile(code)
            }
        }
    }

    static void setProperties(def job, def storage, def git, def srcPath, def v8version, def mail){
        job.with{wrappers{
                environmentVariables {
                    env("STORAGE_FILEPATH", "${storage}".replace("\\", "\\\\"))
                    env("v8version", "${v8version}")
                    env("GIT_URL", "${git}")
                    env("SRC_PATH", "${srcPath}")
                    env("USER_EMAIL", "${mail}")
                }
            }
        }
    }

    static void setInterval(def job, def cronExpr){
        job.with{
            triggers{
                cron('H/15 9-19 * * 1-5')
            }
        }
    }

    static void prepare(def job){
        addShellStep(job, """
call gitsync "%STORAGE_FILEPATH%" %GIT_URL% "%SRC_PATH%" -v8version %v8version% -format plain -email %USER_EMAIL%
""")
    }
}
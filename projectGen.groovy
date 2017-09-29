import groovy.json.JsonSlurperClassic
import java.nio.channels.FileChannel
import ru.svyaznoy.jenkins1c.BuildSteps
import ru.svyaznoy.jenkins1c.GitSync
import ru.svyaznoy.jenkins1c.DeploySteps


def projectsFile = readFileFromWorkspace('projects.json');
def projects = new JsonSlurperClassic().parseText(projectsFile)

for (project in projects) {
    
    def storages = [:]
    def builds = [:]

    // sync
    for (storage in project.storages) {
        // used in builds
        storages[storage.tag] = storage
        
        if(storage.sync != null){
            syncJob = job("${project.name} Git Sync $storage.tag")
            GitSync.setProperties(syncJob,
                storage.filePath,
                project.repos[storage.sync.repo].url,
                storage.sync.srcPath,
                project.v8version,
                storage.sync.email)
            GitSync.setInterval(syncJob, storage.sync.interval)
            GitSync.prepare(syncJob)
        }
    }

    // builds
    int i = 0
    for(build in project.builds){
        def jobName = "${sprintf("%02d", ++i)}. ${project.name} ${build.name}"
        def job = job(jobName)
        build.jobName = jobName
        builds[build.tag] = build

        BuildSteps.preBuild(job);
        BuildSteps.setProperties(job, 
            storages[build.storage].filePath,
            project.name,
            project.v8version);

        BuildSteps.addGitSource(job,
            project.repos[build.repo].url,
            "master")

        BuildSteps.addStandardSteps(job, project)
        BuildSteps.addStandardPublishers(job)
    }

    // deployments
    for(deploy in project.deployments) {
        def job = job("${sprintf("%02d", ++i)}. ${project.name} ${deploy.name}")

        DeploySteps.setProperties(job, project.v8version, deploy.target)
        def upstreamJobName = builds[deploy.srcBuild].jobName
        
        if(deploy.auto == true) {
            DeploySteps.setAutoDeploy(job, upstreamJobName)
        }

        DeploySteps.addArtifactDownload(job, upstreamJobName)
        DeploySteps.addKillSessionsStep(job)
        if(deploy.steps == null)
            DeploySteps.addStandardSteps(job)
        else
            DeploySteps.dispatchDeploySteps(job, deploy.steps)
            
        DeploySteps.addAllowSessionsStep(job)

    }

    // Publish Job
    if(project.publish != null) {
        def job = job("${sprintf("%02d", ++i)}. ${project.name} Publish to support")

        job.with {
            wrappers{
                preBuildCleanup {
                    includePattern('*.zip')
                }
            }
        }

        def upstreamJobName = builds[project.publish].jobName

        DeploySteps.addArtifactDownload(job, upstreamJobName)

        job.with {
            steps {
                artifactDeployer {
                    includes('*.zip')
                    remoteFileLocation("\\\\Fs-msk-n0001\\ci\\Обменник\\${project.name}")
                }
            } 
        }

    }

    // view
    listView(project.name){
    description("$project.name build and publish")
    filterBuildQueue()
        filterExecutors()
        jobs{
            regex("(\\d\\d\\. $project.name.+)|$project.name Git.+")
        }
        columns{
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            testResult(2)
            buildButton()
            lastBuildConsole()
        }
    }
}
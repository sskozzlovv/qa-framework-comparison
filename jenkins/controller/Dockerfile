# Jenkins controller: LTS + plugins + Configuration as Code. Runs NO builds itself (0 executors) - builds go to the agent.
FROM jenkins/jenkins:2.580.2-lts-jdk21

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

USER root
# the repository is mounted from the host (another owner) - let git read it
RUN git config --system --add safe.directory '*'
USER jenkins

# - no setup wizard: everything comes from casc/jenkins.yaml
# - ALLOW_LOCAL_CHECKOUT: jobs clone file:///repo.git (the repo mounted from this computer), blocked by default
# - empty CSP: archived HTML (Playwright report via HTML Publisher) may run its JavaScript. LOCAL STAND ONLY:
#   on a real Jenkins an admin sets up a Resource Root URL instead of switching the protection off.
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false -Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true -Dhudson.model.DirectoryBrowserSupport.CSP="
ENV CASC_JENKINS_CONFIG=/casc/jenkins.yaml

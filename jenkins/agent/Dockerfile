# Jenkins agent: the controller connects over SSH and runs builds here.
#   Selenide + TestNG: JDK 21 (in the base image) + ./mvnw - browsers are in Selenium Grid, not here.
#   Playwright TS:     Node.js + Google Chrome with its system libraries - the browser runs HERE.
FROM jenkins/ssh-agent:9.3.3-jdk21

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends git curl unzip ca-certificates nodejs npm \
    && rm -rf /var/lib/apt/lists/*
# Allure 2 commandline for the Jenkins Allure plugin (tool "allure" with home /opt/allure in casc/jenkins.yaml).
# Pre-installed instead of the plugin's auto-installer: its version list comes from the update center and lags behind.
RUN curl -fsSL -o /tmp/allure.zip https://repo1.maven.org/maven2/io/qameta/allure/allure-commandline/2.46.1/allure-commandline-2.46.1.zip \
    && unzip -q /tmp/allure.zip -d /opt && mv /opt/allure-2.46.1 /opt/allure && rm /tmp/allure.zip
# Google Chrome + its libraries (playwright.config.ts uses channel: 'chrome')
RUN npx -y playwright@1.64.1 install --with-deps chrome \
    && rm -rf /var/lib/apt/lists/* /root/.npm
# ffmpeg: Playwright records video with it (video: 'retain-on-failure'); without it every test fails at
# "browserContext.newPage: Executable doesn't exist .../ffmpeg-linux". Installed for the jenkins user: Playwright
# looks in ~/.cache/ms-playwright of the user that runs the tests.
RUN su jenkins -s /bin/bash -c "npx -y playwright@1.64.1 install ffmpeg" && rm -r /home/jenkins/.npm/_npx
# the repository is mounted from the host (another owner) - let git read it
RUN git config --system --add safe.directory '*'
# caches survive between builds (named volumes in docker-compose.yml); created here so the jenkins user owns them
RUN mkdir -p /home/jenkins/.m2 /home/jenkins/.npm /home/jenkins/agent && chown -R jenkins:jenkins /home/jenkins

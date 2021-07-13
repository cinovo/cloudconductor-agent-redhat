FROM centos-7-java

RUN mkdir -p /opt/cloudconductor-agent/log

COPY package/helperscripts /opt/cloudconductor-agent/scripts
COPY cloudconductor-agent.properties /opt/cloudconductor-agent/cloudconductor-agent.properties
COPY target/lib /opt/cloudconductor-agent/lib
COPY target/de.cinovo.cloudconductor.cloudconductor-agent-redhat.jar /opt/cloudconductor-agent/cloudconductor-agent.jar

WORKDIR /opt/cloudconductor-agent
CMD ["java", "-Xmx64m", "-XX:+ExitOnOutOfMemoryError", "-DdevelopmentMode=false", "-DstartupMode=run", "-jar", "cloudconductor-agent.jar"]

# Specification for docker image
#
# Run and deploy by following the instructions in the Makefile

FROM ubuntu:bionic
# Ubuntu 18.04

#######################
# BASIC PREREQUISITES #
#######################

RUN apt-get update && apt-get install -y \
		build-essential \
		wget \
		git \
		tar \
		vim \
		sed \
		python3 \
		python3-pip
RUN pip3 install pandas

########
# JAVA #
########

RUN apt-get update && apt-get install -y \
	openjdk-8-jdk \
	maven

# set environment variables (needed for apron installation)
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

#########
# APRON #
#########

# install basic APRON prerequisites (following https://github.com/antoinemine/apron)
RUN apt-get update && apt-get install -y \
		gcc \
		libgmp-dev \
		libmpfr-dev \
		perl \
		sed \
		m4

ENV APRON /opt/apron

# install APRON: https://github.com/antoinemine/apron
# - commit label import-svn1104
# - JAVA_HOME: needed in case of sudo
# - sed: bugfix in ./configure (handle special characters in javadoc)
# - sed: bugfix in Makefile.config (locate jni.h)
RUN mkdir -p $APRON && \
		git clone https://github.com/antoinemine/apron.git $APRON && \
		cd $APRON && \
		git checkout import-svn1104 && \
		sed -i -e "s#JAVADOC   = .*#JAVADOC   = \$javadoc -encoding UTF-8#g" ./configure && \
		./configure --help && \
		JAVA_HOME=$JAVA_HOME ./configure -no-ppl && \
		sed -i -e "s#JNIINC    =.*#JNIINC    = -I$JAVA_HOME/include -I$JAVA_HOME/include/linux#g" Makefile.config && \
		make && \
		make install

# generate apron documentation
RUN cd $APRON/japron && \
	make doc && \
	jar cvf japron-sources.jar . .

# add generated jars to maven
# - WARNING: do not use sudo when you run this on your host
RUN mvn install:install-file \
	-Dfile=$APRON/japron/gmp.jar \
	-DgroupId=gmp \
	-DartifactId=gmp \
	-Dversion=0.1 \
	-Dpackaging=jar \
	-DgeneratePom=true \
	-Durl=file://$APRON/japron/gmp \
	-Dsources=$APRON/japron/japron-sources.jar

# WARNING: do not use sudo when you run this on your host
RUN mvn install:install-file \
	-Dfile=$APRON/japron/apron.jar \
	-DgroupId=apron \
	-DartifactId=apron \
	-Dversion=0.1 \
	-Dpackaging=jar \
	-DgeneratePom=true \
	-Durl=file://$APRON/japron/apron \
	-Dsources=$APRON/japron/japron-sources.jar

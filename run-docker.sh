#!/bin/bash

# =================================================================
# DO NOT MODIFY THIS FILE (we will overwrite this file for grading)
# =================================================================
#
# Run a command in the docker image. If no command is provided, runs the image
# interactively, as a shell
#
# Run as: ./run-docker.sh COMMAND



# all commands involving docker use sudo, which is typically required to use
# docker commands


# navigate to the directory containing this script
cd "$(dirname "$0")"

# prepare variables
IMAGE=ethsrilab/rse-project-2020:version-1.2
PROJECTROOT="$(pwd -P)"

# update docker image
sudo docker pull $IMAGE

# run docker
# --rm: removes the container after exiting
# -v: mount the project root under /project
# --workdir: move to /project
if [ $# -eq 0 ]; then
	# interactive mode
	sudo docker run \
		--rm \
		-it \
		-v "$PROJECTROOT":/project \
		--workdir="/project/analysis" \
		$IMAGE
else # run command
	sudo docker run \
		--rm \
		-v "$PROJECTROOT":/project \
		--workdir="/project/analysis" \
		$IMAGE \
		bash -c "$@"
fi

# record exit status
RETVAL=$?

# set the owner of all files created by docker to USER
echo "Resetting owner of directories and files to $USER..."
sudo chown -R $USER:$USER .

# use exit status of docker
exit $RETVAL
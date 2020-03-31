# all commands involving docker use sudo, which is typically required to use docker commands

IMAGE := ethsrilab/rse-project-2020:version-1.2
CONTAINER := rse-project-2020

# build the docker image
.PHONY: image
image:
	sudo docker build -t $(IMAGE) .

.PHONY: details
details: image
	sudo docker history --no-trunc $(IMAGE)

# launch a docker container using the image, which provides a shell in the
# container
.PHONY: run
run: image
	sudo docker run -it --rm --name $(CONTAINER) $(IMAGE)

# Log in to the docker registry (needed to deploy the image)
.PHONY: login
login:
	sudo docker login

# Deploy the image
.PHONY: deploy
deploy: login image
	sudo docker push $(IMAGE)

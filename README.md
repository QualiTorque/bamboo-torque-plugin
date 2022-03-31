# Bamboo-Torque-Plugin

## Intro

When a developer commits code to the source control repository, a new build is triggered and a new artifact is created.
While testing, CloudShell Torque provides on-demands sandbox environments integrating with test tools and storage
providers to pull the artifacts and push to Production.

Bamboo-Torque-Plugin integrates CloudShell Torque into your Bamboo plan. You can use the available build tasks to create
a sandbox from any blueprint, start your tests and end the sandbox when finished.

## Installation

1) Download the plugin jar in marketplace

2) Navigate to the add-ons section in Bamboo administration page

3) Upload the jar file into the "Upload add-on" section

## Configuring CloudShell Torque in Bamboo

1) Generate an API token in CloudShell Torque

2) Open Bamboo administration page

3) Open "CloudShell Torque Settings"

3) Fill up all required fields.

![Alt text](pics/bamboo-admin.png?raw=true)

### CloudShell Torque plan tasks

After installation you will have three tasks added to the task list available in Bamboo:

- Start Torque Sandbox Task
- Wait for Torque Sandbox Task
- End Torque Sandbox Task
![Alt text](pics/torque-tasks.png?raw=true)

### Launching a Sandbox

1) Add the Start Torque Start Sandbox task to your pipeline.
![Alt text](pics/start-task.png?raw=true)

 - **Space name** - enter a name for your CloudShell Torque space.
 - **Blueprint name** - Enter the name of the blueprint you would like to use for creating this sandbox.
 - **Sandbox name** - Enter a name for the sandbox
 - **Artifacts** - If this blueprint has artifacts, you may specify them in a comma separated list of artifact names and their values. e.g., artifact1 name=value1, artifact2 name=value2
 - **Inputs** - If this blueprint has inputs, you may specify them in a comma separated list of input names and their values. e.g., input1 name=value1, input2 name=value2

This task uses **_${bamboo.SANDBOX_ID}_** variable to return the identifier of sandbox.

2) Add the Wait For Torque Sandbox task.

![Alt text](pics/wait-task.png?raw=true)

 - **Space** - enter a name for your CloudShell Torque space.
 - **Tiemeout** - Set the timeout for this step, if your sandbox will not be ready when the timeout is reached,
 Bamboo will abort the deployment.
 
 If ready sandbox has quick links, they will be stored in the following bamboo variables:
 _**${bamboo.endpoint0}, ..., ${bamboo.endpointN}**_.
 
### Ending a Sandbox from your Pipeline
Add the End Torque Sandbox task to plan.

![Alt text](pics/end-task.png?raw=true)

 - **Space Name** - Enter the name of your CloudShell Torque space.

#### Note: CloudShell Torque Bamboo plugin supports only one Sandbox per job.

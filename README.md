# Bamboo Torque Plugin

## Intro

This README explains how to install, configure, and use the __bamboo-torque-plugin__.
When a developer commits code to the source control repository, a new build is triggered and a new artifact is created.
While testing, Torque provides on-demand sandbox environments integrating with test tools and storage
providers to pull the artifacts and push to Production.

__Bamboo Torque Plugin__ integrates Torque into your Bamboo plan. You can use the available build tasks to create
a sandbox from any blueprint, start your tests and end the sandbox when finished.

## Installation

1) In the __Bamboo administration__ page, scroll down to the __Manage apps__ section and click __Find new apps__.

2) Search for __Torque__ and install the __Bamboo Torque Plugin__.

## Configuration

1) Generate an API token in Torque: 
<br>a. In the desired Torque space, go to the __Settings > Integrations__ page and click __Bamboo__.
<br>b. Click the __New Token__ button.
<br>c. Copy the generated token.

2) Open the __Bamboo administration__ page.

3) Open __Torque Settings__.

4) Fill in all fields.
<br>![Alt text](pics/bamboo-admin.png?raw=true)

## Torque plan tasks

After installation, you will have three tasks added to your Bamboo task list:

- [Start Torque Sandbox](#start-torque-sandbox)
- [Wait for Torque Sandbox](#wait-for-torque-sandbox)
- [End Torque Sandbox](#end-torque-sandbox)

![Alt text](pics/torque-tasks.png?raw=true)

### Start Torque Sandbox

1) Add the __Start Torque Sandbox__ task.
![Alt text](pics/start-task.png?raw=true)
<br> * **Space name** - enter a name for your Torque space.
<br> *  **Blueprint name** - Enter the name of the blueprint you would like to use for creating this sandbox.
<br> *  **Sandbox name** - Enter a name for the sandbox
<br> *  **Artifacts** - If this blueprint has artifacts, you may specify them in a comma separated list of artifact names and their values. e.g., artifact1 name=value1, artifact2 name=value2
<br> *  **Inputs** - If this blueprint has inputs, you may specify them in a comma separated list of input names and their values. e.g., input1 name=value1, input2 name=value2

This task uses the **_${bamboo.SANDBOX_ID}_** variable to return the identifier of sandbox.

### Wait for Torque Sandbox

The __Wait for Sandbox__ task is used to wait for the sandbox to become Active. While the sandbox is launching, it cannot be used and the application links are unavailable.

* Add the Wait For Torque Sandbox task.
![Alt text](pics/wait-task.png?raw=true)
<br>* **Space** - enter a name for your Torque space.
<br>* **Timeout** - Set the timeout for this step, if your sandbox will not be ready when the timeout is reached,
 Bamboo will abort the deployment.
 
 If the active sandbox has quick links, they will be stored in the following bamboo variables:
 _**${bamboo.endpoint0}, ..., ${bamboo.endpointN}**_.
 
### End Torque Sandbox
* Add the __End Torque Sandbox__ task to your plan.
![Alt text](pics/end-task.png?raw=true)
<br> * **Space Name** - Enter the name of your Torque space.

#### Note: Torque Bamboo plugin supports only one Sandbox per job.

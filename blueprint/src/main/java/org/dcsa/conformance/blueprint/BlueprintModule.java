package org.dcsa.conformance.blueprint;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import java.io.File;
import java.io.IOException;

/**
 * Blueprint module for creating BPMN processes using Camunda BPMN Model library.
 */
public class BlueprintModule {

    /**
     * Creates an example BPMN process with a start event, user task, and end event.
     *
     * @return the created BPMN model instance
     */
    public BpmnModelInstance createExampleProcess() {
        // Create a new BPMN model instance
        BpmnModelInstance modelInstance = Bpmn.createEmptyModel();

        // Create process definition
        org.camunda.bpm.model.bpmn.instance.Definitions definitions = modelInstance.newInstance(org.camunda.bpm.model.bpmn.instance.Definitions.class);
        definitions.setTargetNamespace("http://dcsa.org/conformance/blueprint");
        modelInstance.setDefinitions(definitions);

        // Create process
        Process process = modelInstance.newInstance(Process.class);
        process.setId("exampleProcess");
        process.setName("Example BPMN Process");
        process.setExecutable(true);
        definitions.addChildElement(process);

        // Create start event
        StartEvent startEvent = modelInstance.newInstance(StartEvent.class);
        startEvent.setId("startEvent");
        startEvent.setName("Process Started");
        process.addChildElement(startEvent);

        // Create user task
        UserTask userTask = modelInstance.newInstance(UserTask.class);
        userTask.setId("reviewTask");
        userTask.setName("Review and Approve");
        process.addChildElement(userTask);

        // Create end event
        EndEvent endEvent = modelInstance.newInstance(EndEvent.class);
        endEvent.setId("endEvent");
        endEvent.setName("Process Completed");
        process.addChildElement(endEvent);

        // Create sequence flows
        SequenceFlow flow1 = modelInstance.newInstance(SequenceFlow.class);
        flow1.setId("flow1");
        flow1.setSource(startEvent);
        flow1.setTarget(userTask);
        process.addChildElement(flow1);

        SequenceFlow flow2 = modelInstance.newInstance(SequenceFlow.class);
        flow2.setId("flow2");
        flow2.setSource(userTask);
        flow2.setTarget(endEvent);
        process.addChildElement(flow2);

        return modelInstance;
    }

    /**
     * Saves a BPMN model instance to a file.
     *
     * @param modelInstance the BPMN model to save
     * @param filePath the path where the BPMN file should be saved
     * @throws IOException if there's an error writing the file
     */
    public void saveBpmnToFile(BpmnModelInstance modelInstance, String filePath) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs(); // Ensure parent directories exist
        Bpmn.writeModelToFile(file, modelInstance);
    }

    /**
     * Main method to demonstrate BPMN process creation and saving.
     */
    public static void main(String[] args) {
        BlueprintModule blueprint = new BlueprintModule();

        // Create example process
        BpmnModelInstance modelInstance = blueprint.createExampleProcess();

        // Define output path
        String outputPath = "blueprint/generated-resources/example-process.bpmn";

        try {
            // Save to file
            blueprint.saveBpmnToFile(modelInstance, outputPath);
            System.out.println("BPMN process successfully created and saved to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error saving BPMN file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


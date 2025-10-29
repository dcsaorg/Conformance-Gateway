package org.dcsa.conformance.blueprint;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the blueprint module BPMN process creation.
 */
class BlueprintModuleTest {

    @Test
    void testCreateExampleProcess() {
        BlueprintModule blueprint = new BlueprintModule();

        // Create the process
        BpmnModelInstance modelInstance = blueprint.createExampleProcess();

        // Verify the model is not null
        assertNotNull(modelInstance);

        // Verify the process exists and has correct properties
        Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);
        assertEquals(1, processes.size());

        Process process = processes.iterator().next();
        assertEquals("exampleProcess", process.getId());
        assertEquals("Example BPMN Process", process.getName());
        assertTrue(process.isExecutable());
    }

    @Test
    void testProcessContainsExpectedElements() {
        BlueprintModule blueprint = new BlueprintModule();
        BpmnModelInstance modelInstance = blueprint.createExampleProcess();

        // Verify start event exists
        Collection<StartEvent> startEvents = modelInstance.getModelElementsByType(StartEvent.class);
        assertEquals(1, startEvents.size());
        assertEquals("startEvent", startEvents.iterator().next().getId());

        // Verify user task exists
        Collection<UserTask> userTasks = modelInstance.getModelElementsByType(UserTask.class);
        assertEquals(1, userTasks.size());
        assertEquals("reviewTask", userTasks.iterator().next().getId());

        // Verify end event exists
        Collection<EndEvent> endEvents = modelInstance.getModelElementsByType(EndEvent.class);
        assertEquals(1, endEvents.size());
        assertEquals("endEvent", endEvents.iterator().next().getId());
    }

    @Test
    void testSaveBpmnToFile(@TempDir Path tempDir) throws IOException {
        BlueprintModule blueprint = new BlueprintModule();
        BpmnModelInstance modelInstance = blueprint.createExampleProcess();

        // Save to temporary file
        String filePath = tempDir.resolve("test-process.bpmn").toString();
        blueprint.saveBpmnToFile(modelInstance, filePath);

        // Verify file was created
        File savedFile = new File(filePath);
        assertTrue(savedFile.exists());
        assertTrue(savedFile.isFile());
        assertTrue(savedFile.length() > 0);
    }
}


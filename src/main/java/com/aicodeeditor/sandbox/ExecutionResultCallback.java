package com.aicodeeditor.sandbox;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Callback for capturing execution results from Docker containers
 */
public class ExecutionResultCallback extends ExecStartResultCallback {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionResultCallback.class);
    
    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    
    @Override
    public void onNext(Frame frame) {
        try {
            switch (frame.getStreamType()) {
                case STDOUT:
                case RAW:
                    stdout.write(frame.getPayload());
                    break;
                case STDERR:
                    stderr.write(frame.getPayload());
                    break;
                default:
                    logger.debug("Unknown stream type: {}", frame.getStreamType());
            }
        } catch (IOException e) {
            logger.error("Error processing frame", e);
        }
        super.onNext(frame);
    }
    
    /**
     * Get combined output (stdout + stderr)
     */
    public String getOutput() {
        StringBuilder output = new StringBuilder();
        
        String stdoutStr = stdout.toString(StandardCharsets.UTF_8);
        String stderrStr = stderr.toString(StandardCharsets.UTF_8);
        
        if (!stdoutStr.isEmpty()) {
            output.append("STDOUT:\n").append(stdoutStr).append("\n");
        }
        
        if (!stderrStr.isEmpty()) {
            output.append("STDERR:\n").append(stderrStr).append("\n");
        }
        
        return output.length() > 0 ? output.toString() : "No output";
    }
    
    /**
     * Get stdout only
     */
    public String getStdout() {
        return stdout.toString(StandardCharsets.UTF_8);
    }
    
    /**
     * Get stderr only
     */
    public String getStderr() {
        return stderr.toString(StandardCharsets.UTF_8);
    }
    
    /**
     * Check if there were any errors
     */
    public boolean hasErrors() {
        return stderr.size() > 0;
    }
}

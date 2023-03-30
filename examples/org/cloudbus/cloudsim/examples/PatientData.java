package org.cloudbus.cloudsim.examples;

public class PatientData {
    public int patientId;
    public int patientPriority;
    public int patientTimeIn;
    public int patientDuration;

    public PatientData(int patientTimeIn, int patientId, int patientPriority, int patientDuration) {
        this.patientId = patientId;
        this.patientPriority = patientPriority;
        this.patientTimeIn = patientTimeIn;
        this.patientDuration = patientDuration;
    }
}

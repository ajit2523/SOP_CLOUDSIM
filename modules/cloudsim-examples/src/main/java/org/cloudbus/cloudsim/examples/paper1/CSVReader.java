package org.cloudbus.cloudsim.examples.paper1;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CSVReader {
    public static List<PatientData> readCSV(String fileName) {;
        List<PatientData> patientDataList = new ArrayList<>();
        try {
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);
            List<String[]> rows = new ArrayList<>();

            String heading = scanner.nextLine();

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] data = line.split(",");
                rows.add(data);
            }

            scanner.close();

            // Process the data from the CSV file
            for (String[] row : rows) {
                int patientTimeIn = Integer.parseInt(row[0]);
                int patientId = Integer.parseInt(row[1]);
                int patientPriority = Integer.parseInt(row[2]);
                int patientDuration = Integer.parseInt(row[3]);
                patientDataList.add(new PatientData(patientTimeIn, patientId, patientPriority, patientDuration));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return patientDataList;
    }
}

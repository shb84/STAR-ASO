/*
 * The purpose of this class is to perform actions on a CSV file. 
 */

package framework;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author shb
 */
public class CSVFile {
    
    // --- Properties 
    private 
            String m_filepath; 
            List<String[]> csvtable;
            File inputFile;
            int numRows; 
            int numCols;
    
    // --- Constructor 
    public CSVFile(String filepath){
        m_filepath = filepath;
        try {
            inputFile = new File(m_filepath);
            try (CSVReader reader = new CSVReader(new FileReader(inputFile), ',')) {
                csvtable = reader.readAll();
            } 
            numRows = csvtable.size(); 
            numCols = csvtable.get(0).length;
        }
        catch (IOException e) {
            System.out.println("Caught IOException: " + e.getMessage()); 
        }
    }
    
    // --- Methods 
    
    // -------------------------------------------------------------------------
    // ---------------------- P U B L I C   M E T H O D S ----------------------
    // -------------------------------------------------------------------------
    
    // This method updates a specific cell (row,col) in a CSV file (URL code) 
    // http://stackoverflow.com/questions/4397907/updating-specific-cell-csv-file-using-java
    public void update(double value,int row, int col) throws IOException {
        update(String.valueOf(value),row,col); 
    }
    public void update(String str,int row, int col) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(inputFile), ',')) {
            csvtable = reader.readAll();
            csvtable.get(row)[col] = str; 
        }
        try (CSVWriter writer = new CSVWriter(new FileWriter(inputFile), ',',CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER)) {
            writer.writeAll(csvtable);
            writer.flush();
        }
    }

     // This methods extracts the specified column from the CSV file 
     public String[] getColumn(int columnNumber){
         String columnName = getColumnName(columnNumber); 
         return getColumn(columnName); 
     } 
     public String[] getColumn(String columnName){ 
            
        // Argument to be returned 
        String[] series; 
        
        // Try catch loop for reading CSV file
        try {
            // Read file 
            CSVReader reader = new CSVReader(new FileReader(inputFile), ',');
            csvtable = reader.readAll();
            
            // Extract headers 
            String[] headers = csvtable.get(0); 
            
            // Initialize series 
            series = new String[numRows-1]; //Subtract header row from count
            
            // Loop but only read column where header matches columnName
            for (int row = 0; row < numRows-1; row++){
                for (int col = 0; col < numCols; col++){
                     if (headers[col].equals(columnName)){
                         series[row] = csvtable.get(row+1)[col]; 
                     }
                }
            }
            reader.close();
        }
        catch (IOException e) {
            series = new String[1]; 
            System.out.println("Caught IOException: " + e.getMessage()); 
        }
        return series; 
    }
     
    // Given a column, and assuming the item is unique, this method return the 
    // first row in which the item is found. Note that counting starts at 0. 
    public int findRowContainingItem(String columnName, String itemName){
        String[] items = getColumn(columnName); 
        int row = 1; // items does not include header (hence start at 1)
        int count = row; 
        for (String item: items){
            if (item.equals(itemName)){row = count;}
            count++;
        }
        return row; 
    }
    
    // This method returns the first column number of a column header
    public int getColumnNumber(String targetHeader){
        int numCols = csvtable.get(0).length;
        String[] headers = csvtable.get(0); 
        int col = 0;
        for (int i = 0; i < numCols; i++){
            if (headers[i].equals(targetHeader)){col = i;}
        }
        return col; 
    }
    
    // This method returns the first column number of a column header
    public String getColumnName(int columnNumber){
        return csvtable.get(0)[columnNumber]; 
    }

    // -------------------------------------------------------------------------
    // ---------------------- P R O T E C T E D   M E T H O D S ----------------
    // -------------------------------------------------------------------------
    
    // -------------------------------------------------------------------------
    // ---------------------- P R I V A T E   M E T H O D S --------------------
    // -------------------------------------------------------------------------
}

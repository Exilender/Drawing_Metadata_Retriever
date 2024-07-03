/*
 * Drawing Metadata Retriever was created to help me see what metadata I can pull from my digital drawings.
 * Created by Dane (Sara) Wright
 * 07/01/2024
 */

 package com.exilender;

 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 import java.nio.file.attribute.BasicFileAttributes;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Scanner;
 import java.util.stream.Stream;

 import com.drew.imaging.ImageMetadataReader;
 import com.drew.imaging.ImageProcessingException;
 import com.drew.metadata.Directory;
 import com.drew.metadata.Metadata;
 import com.drew.metadata.Tag;
 import com.opencsv.CSVWriter;

 public class DrawingMetadataRetriever {
     public static void main(String[] args) {
         // Asks for folder path to search through and specifies the output file name
         String outputFile = "DrawingMetadataRetrieved.csv";
         Scanner scanner = new Scanner(System.in);

         try (CSVWriter write = new CSVWriter(new FileWriter(outputFile))) {
             System.out.print("Enter location you'd like to search for Drawing Metadata in: ");
             String folderPath = scanner.nextLine();
             scanner.close();

             // Writing header
             String[] header = {"File", "Width", "Height", "Artist", "Copyright", "Filesize (bytes)"};
             write.writeNext(header);

             List<Path> parentDirFiles = new ArrayList<>();
             List<Path> subDirFiles = new ArrayList<>();

             try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
                 // Have to make sure file is regular file and also a Picture
                 paths.filter(Files::isRegularFile)
                     .filter(path -> isPicture(path))
                     .forEach(path -> {
                         if (path.getParent().equals(Paths.get(folderPath))) {
                             parentDirFiles.add(path);
                         } else {
                             subDirFiles.add(path);
                         }
                     });

                 // For Writing the meta data of all files in Parent directory first, then going into the Subdirectories.
                 String[] Folder = {""};
                 boolean isFirstFolder = true;
                 for (Path path : parentDirFiles) {
                     String folderLocation = path.getParent().toString();
                     if (!folderLocation.equals(Folder[0])) {
                         if (!isFirstFolder) {
                             // Add a blank line for all but the first folder
                             write.writeNext(new String[]{""});
                         }
                         // Write the folder location as a section header
                         write.writeNext(new String[]{folderLocation});
                         Folder[0] = folderLocation;
                         isFirstFolder = false;
                     }
                     getImageMetadata(path, write);
                 }

                 // Write metadata of files in the subdirectories
                 for (Path path : subDirFiles) {
                     String folderLocation = path.getParent().toString();
                     if (!folderLocation.equals(Folder[0])) {
                         // Add a blank line
                         write.writeNext(new String[]{""});
                         // Write the folder location as a section header
                         write.writeNext(new String[]{folderLocation});
                         Folder[0] = folderLocation;
                     }
                     getImageMetadata(path, write);
                 }

             } catch (IOException e) {
                 System.err.println("Error finding the chosen directory: " + e.getMessage());
             }
         } catch (IOException e) {
             System.err.println("Error writing to CSV file: " + e.getMessage());
         }
     }

     private static boolean isPicture(Path path) {
         // Makes sure to get images because it kept trying to read files that could not be read by metadata-extractor
         String[] imageExt = {".jpg", ".jpeg", ".png", ".gif", ".tiff"};
         String fileName = path.getFileName().toString().toLowerCase();

         for (String ext : imageExt) {
             if (fileName.endsWith(ext)) {
                 return true;
             }
         }
         return false;
     }

     private static void getImageMetadata(Path path, CSVWriter write) {
         // Getting the actual metadata of the Picture files in the chosen directory & writing them to CSV
         try {
             BasicFileAttributes bfa = Files.readAttributes(path, BasicFileAttributes.class);

             String width = "";
             String height = "";
             String artist = "";
             String copyright = "";
             String filesize = Long.toString(bfa.size());

             File file = path.toFile();
             Metadata metadata = ImageMetadataReader.readMetadata(file);

             for (Directory directory : metadata.getDirectories()) {
                 for (Tag tag : directory.getTags()) {
                     switch (tag.getTagName()) {
                         case "Image Width" -> width = tag.getDescription();
                         case "Image Height" -> height = tag.getDescription();
                         case "Artist" -> artist = tag.getDescription();
                         case "Copyright" -> copyright = tag.getDescription();
                     }
                 }
             }

             String[] foundData = {path.getFileName().toString(),
                 width.replaceAll("[^0-9]", ""),
                 height.replaceAll("[^0-9]", ""),
                 artist,
                 copyright,
                 filesize};
             write.writeNext(foundData);

         } catch (ImageProcessingException | IOException e) {
             System.err.println("Error reading image metadata: " + e.getMessage());
         }
     }
 }
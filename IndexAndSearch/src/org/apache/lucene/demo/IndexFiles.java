package org.apache.lucene.demo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.universalchardet.UniversalDetector;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {
  
  private IndexFiles() {}
  //===========Start==========
  public static Map<String, String> pageRankMap = new HashMap<String, String>();
  //===========End============
  /** Index all text files under a directory. */
  public static void main(String[] args) {
    String usage = "java org.apache.lucene.demo.IndexFiles"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    String indexPath = "index";
    String docsPath = null;
    boolean create = true;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      } else if ("-update".equals(args[i])) {
        create = false;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final Path docDir = Paths.get(docsPath);
    if (!Files.isReadable(docDir)) {
      System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    
    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(Paths.get(indexPath));
      //Analyzer analyzer = new StandardAnalyzer();
      Analyzer analyzer = new ThaiAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      if (create) {
        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);
      } else {
        // Add new documents to an existing index:
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      }

      // Optional: for better indexing performance, if you
      // are indexing many documents, increase the RAM
      // buffer.  But if you do this, increase the max heap
      // size to the JVM (eg add -Xmx512m or -Xmx1g):
      //
      // iwc.setRAMBufferSizeMB(256.0);

      //========= start ========
      String pageRankResultPath = "E:/KU/Year4/IR/HwWs3-PageRank/pageRankResult.txt";
      Path pageRankResultFile = Paths.get(pageRankResultPath);
      String pageRankResult = readFile(pageRankResultFile.toString(),StandardCharsets.UTF_8);
      //System.out.println(pageRankResult);
      //Map<String, Float> pageRankMap = new HashMap<String, Float>();
      BufferedReader bufReader = new BufferedReader(new StringReader(pageRankResult));
      String linePageRankResult=null;
      while( (linePageRankResult=bufReader.readLine()) != null )
      {
    	  //System.out.println(linePageRankResult);
    	  String[] linePageRankResultParts = linePageRankResult.split(":pr:");
    	  //System.out.println(linePageRankResultParts[0]);
    	  pageRankMap.put(linePageRankResultParts[0], linePageRankResultParts[1]);
    	  //System.out.println(pageRankMap.get(linePageRankResultParts[0]));
      }
      
      //========= end ==========
      
      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir);

      // NOTE: if you want to maximize search performance,
      // you can optionally call forceMerge here.  This can be
      // a terribly costly operation, so generally it's only
      // worth it when your index is relatively static (ie
      // you're done adding documents to it):
      //
      // writer.forceMerge(1);

      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   * 
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *  
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param path The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(final IndexWriter writer, Path path) throws IOException {
    if (Files.isDirectory(path)) {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          try {
            indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
          } catch (IOException ignore) {
            // don't index files that can't be read.
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } else {
      indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
    }
  }

  //====== Start ======
  public static String readFile(String path, Charset encoding) throws IOException 
  {
 	byte[] encoded = Files.readAllBytes(Paths.get(path));
 	return new String(encoded, encoding);
  }
  
  /*
  public static String html2text(String html) {
	    return Jsoup.parse(html).text();
  }*/
  //====== End ========
  
  
  /** Indexes a single document */
  static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
    try (InputStream stream = Files.newInputStream(file)) {
      // make a new, empty document
      Document doc = new Document();
      
      // Add the path of the file as a field named "path".  Use a
      // field that is indexed (i.e. searchable), but don't tokenize 
      // the field into separate words and don't index term frequency
      // or positional information:
      Field pathField = new StringField("path", file.toString(), Field.Store.YES);
      doc.add(pathField);
      
      // Add the last modified date of the file a field named "modified".
      // Use a LongField that is indexed (i.e. efficiently filterable with
      // NumericRangeFilter).  This indexes to milli-second resolution, which
      // is often too fine.  You could instead create a number based on
      // year/month/day/hour/minutes/seconds, down the resolution you require.
      // For example the long value 2011021714 would mean
      // February 17, 2011, 2-3 PM.
      doc.add(new LongField("modified", lastModified, Field.Store.NO));
      
      // Add the contents of the file to a field named "contents".  Specify a Reader,
      // so that the text of the file is tokenized and indexed, but not stored.
      // Note that FileReader expects the file to be in UTF-8 encoding.
      // If that's not the case searching for special characters will fail.
      doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
      //doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream))));
      
      //====== Start =======
      
      File docFile = new File(file.toString());
      String title = "";
      String docContents = "";
      String snippet = "";
      String URL = "";
      String pageRank = "";
      try {
    	  org.jsoup.nodes.Document document = Jsoup.parse(docFile, "UTF-8");
    	  Elements title_element = document.select("title");
    	  Elements url_element = document.select("url");  
    	  title = title_element.text();
    	  URL = url_element.text();
    	  docContents = document.text();
    	  //docContents = html2text(readFile(file.toString(),StandardCharsets.UTF_8));
          //docContents = html2text(readFile(file.toString(),Charset.defaultCharset()));
    	  
    	  //====================check encoding start==============================
          /*
    	  byte[] buf = new byte[4096];
          String fileName = file.toString();
          java.io.FileInputStream fis = new java.io.FileInputStream(fileName);
          UniversalDetector detector = new UniversalDetector(null);
          int nread;
          while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
              detector.handleData(buf, 0, nread);
          }
          detector.dataEnd();

          String encoding = detector.getDetectedCharset();
          if (encoding != null) {
              System.out.println("Detected encoding = " + encoding);
          } else {
              System.out.println("No encoding detected.");
          }
          detector.reset();
          */
          //====================check encoding end==============================
    	  
    	  snippet = docContents;
    	  snippet = snippet.substring(URL.length() + title.length(), Math.min(snippet.length(), 1000));
    	  if(pageRankMap.get(URL) != null){
    		  pageRank = pageRankMap.get(URL);
    		  doc.add(new StringField("title", title, Field.Store.YES));
    	      doc.add(new StringField("snippet", snippet, Field.Store.YES));
    	      doc.add(new TextField("docContents", docContents, Field.Store.YES));
    	      doc.add(new StringField("URL", URL, Field.Store.YES));
    	      doc.add(new StoredField("PageRank", pageRank));
    	      //System.out.println(docContents);
    	      //System.out.println();
    	  }else{
    		  return;
    	  }
      }catch(IllegalArgumentException e){ 
    	  return;
      }
      
      
      //====== End =========
      
      if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
        // New index, so we just add the document (no old document can be there):
        System.out.println("adding " + file);
        writer.addDocument(doc);
      } else {
        // Existing index (an old copy of this document may have been indexed) so 
        // we use updateDocument instead to replace the old one matching the exact 
        // path, if present:
        System.out.println("updating " + file);
        writer.updateDocument(new Term("path", file.toString()), doc);
      }
    }
  }
}
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pdfbox.examples.strprinting;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.print.*;
import java.io.File;
import java.io.IOException;

/**
 * Examples of various different ways to print PDFs using PDFBox.
 */
public final class StringPrinting
{
    private StringPrinting()
    {
    }

    /**
     * Entry point.
     */
    public static void main(String[] args) throws PrinterException, IOException
    {
        if (args.length != 1)
        {
            System.err.println("usage: java " + StringPrinting.class.getName() + " <input>");
            System.exit(1);
        }

        String filename = args[0];
        PDDocument document = PDDocument.load(new File(filename));

        print(document);
        document.close();
    }

    /**
     * Prints the document at its actual size. This is the recommended way to print.
     */
    private static void print(PDDocument document) throws PrinterException
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPageable(new StringPDFPageable(document));
        job.print();
    }
}

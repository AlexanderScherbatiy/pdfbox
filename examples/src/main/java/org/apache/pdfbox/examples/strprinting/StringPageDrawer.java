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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import java.util.Map;
import java.util.HashMap;
import java.awt.geom.AffineTransform;
import java.io.IOException;

public class StringPageDrawer extends PageDrawer {

    private static final Log LOG = LogFactory.getLog(StringPageDrawer.class);
    private final Map<PDFont, java.awt.Font> fonts = new HashMap<PDFont, java.awt.Font>();

    StringPageDrawer(PageDrawerParameters parameters) throws IOException {
        super(parameters);
    }

    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
        // fall-back to draw Glyph when awt font has not been found
        // super.showGlyph(textRenderingMatrix, font, code, displacement);

        AffineTransform at = textRenderingMatrix.createAffineTransform();
        at.concatenate(font.getFontMatrix().createAffineTransform());

        java.awt.Graphics2D graphics = getGraphics();

        AffineTransform prevTx = graphics.getTransform();
        stretchNonEmbeddedFont(at, font, code, displacement);
        // Probably relates to DEFAULT_FONT_MATRIX transform from PDFont
        at.scale(100, -100);
        graphics.transform(at);

        graphics.setComposite(getGraphicsState().getNonStrokingJavaComposite());
        // Private methods in PageDrawer
        // graphics.setPaint(getNonStrokingPaint());
        // setClip();
        // Use the black color for the current example
        graphics.setColor(java.awt.Color.BLACK);

        java.awt.Font prevFont = graphics.getFont();
        java.awt.Font awtFont = getAwtFont(font);
        graphics.setFont(awtFont);

        graphics.drawString(font.toUnicode(code), 0, 0);

        graphics.setFont(prevFont);
        graphics.setTransform(prevTx);
    }

    private void stretchNonEmbeddedFont(AffineTransform at, PDFont font, int code,
                                        Vector displacement) throws IOException {
        // Stretch non-embedded glyph if it does not match the height/width contained in the PDF.
        // Vertical fonts have zero X displacement, so the following code scales to 0 if we don't skip it.
        // TODO: How should vertical fonts be handled?
        if (!font.isEmbedded() && !font.isVertical() && !font.isStandard14() && font.hasExplicitWidth(code)) {
            float fontWidth = font.getWidthFromFont(code);
            if (fontWidth > 0 && // ignore spaces
                    Math.abs(fontWidth - displacement.getX() * 1000) > 0.0001) {
                float pdfWidth = displacement.getX() * 1000;
                at.scale(pdfWidth / fontWidth, 1);
            }
        }
    }

    private java.awt.Font cacheFont(PDFont font, java.awt.Font awtFont) {
        fonts.put(font, awtFont);
        return awtFont;
    }

    private java.awt.Font getAwtFont(PDFont font) throws IOException {

        java.awt.Font awtFont = fonts.get(font);

        if (awtFont != null) {
            return awtFont;
        }

        if (font instanceof PDType0Font) {
            return cacheFont(font, getPDType0AwtFont((PDType0Font) font));
        }

        if (font instanceof PDType1Font) {
            return cacheFont(font, getPDType1AwtFont((PDType1Font) font));
        }

        String msg = String.format("Not yet implemented: %s", font.getClass().getName());
        throw new UnsupportedOperationException(msg);
    }

    public java.awt.Font getPDType0AwtFont(PDType0Font font) throws IOException {

        java.awt.Font awtFont = null;
        PDCIDFont descendantFont = font.getDescendantFont();

        if (descendantFont != null) {

            if (descendantFont instanceof PDCIDFontType2) {
                awtFont = getPDCIDAwtFontType2((PDCIDFontType2) descendantFont);
            }
            if (awtFont != null) {
                /*
                 * Fix Oracle JVM Crashes.
                 * Tested with Oracle JRE 6.0_45-b06 and 7.0_21-b11
                 */
                awtFont.canDisplay(1);
            }
        }
        if (awtFont == null) {
            awtFont = FontManager.getStandardFont();
            LOG.info("Using font " + awtFont.getName()
                    + " instead of " + descendantFont.getFontDescriptor().getFontName());
        }
        return awtFont.deriveFont(10f);
    }

    private java.awt.Font getPDType1AwtFont(PDType1Font font) throws IOException {

        java.awt.Font awtFont = null;
        String baseFont = font.getBaseFont();
        PDFontDescriptor fd = font.getFontDescriptor();

        if (fd != null) {
            if (fd.getFontFile() != null) {
                try {
                    // create a type1 font with the embedded data
                    awtFont = java.awt.Font.createFont(java.awt.Font.TYPE1_FONT, fd.getFontFile().createInputStream());
                } catch (java.awt.FontFormatException e) {
                    LOG.info("Can't read the embedded type1 font " + fd.getFontName());
                }
            }
            if (awtFont == null) {
                // check if the font is part of our environment
                if (fd.getFontName() != null) {
                    awtFont = FontManager.getAwtFont(fd.getFontName());
                }
                if (awtFont == null) {
                    LOG.info("Can't find the specified font " + fd.getFontName());
                }
            }
        } else {
            // check if the font is part of our environment
            awtFont = FontManager.getAwtFont(baseFont);
            if (awtFont == null) {
                LOG.info("Can't find the specified basefont " + baseFont);
            }
        }

        if (awtFont == null) {
            // we can't find anything, so we have to use the standard font
            awtFont = FontManager.getStandardFont();
            LOG.info("Using font " + awtFont.getName() + " instead");
        }

        return awtFont.deriveFont(20f);
    }

    public java.awt.Font getPDCIDAwtFontType2(PDCIDFontType2 font) throws IOException {

        java.awt.Font awtFont = null;
        PDFontDescriptor fd = font.getFontDescriptor();
        PDStream ff2Stream = fd.getFontFile2();

        if (ff2Stream != null) {
            try {
                // create a font with the embedded data
                awtFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, ff2Stream.createInputStream());
            } catch (java.awt.FontFormatException f) {
                LOG.info("Can't read the embedded font " + fd.getFontName());
            }
            if (awtFont == null) {
                if (fd.getFontName() != null) {
                    awtFont = FontManager.getAwtFont(fd.getFontName());
                }
                if (awtFont != null) {
                    LOG.info("Using font " + awtFont.getName() + " instead");
                }
            }
        }

        return awtFont;
    }
}


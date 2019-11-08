/*
 * ExposureTime is used to help determine a DSLR shutter close lag
 * Copyright (C) 2018 - 2019  John Murphy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package info.johnmurphyastro.etime;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Moving block used to test DSLR or CCD exposure start and end times
 * @author John Murphy
 */
public class LedGrid extends JComponent implements ActionListener {
    private static final int WINDOW_MIN_HEIGHT = 150;
    private static final int WINDOW_MIN_WIDTH = 980;
    private static final int CANVAS_MIN_HEIGHT = 80;
    private static final int CANVAS_MIN_WIDTH = 330;
    private final Dimension canvasSize = new Dimension(0, 0);
    private Image canvas;
    private Graphics canvasGc;
    
    private static final int TOP_MARGIN = 20;
    private static final int LEFT_MARGIN = 10;
    private static final int SPACE5 = 1;
    private static final int UPDATE_MS = 1;
    private final Rectangle ledRect = new Rectangle();
    private final Rectangle ledOldRect = new Rectangle();
    private final Rectangle repaintArea = new Rectangle();
    private final Rectangle repaintAreas = new Rectangle();
    private final Point gridXY = new Point();
    private final Point oldGridXY = new Point();
    private final Point tmpGridXY = new Point();
    private int ledSize = 1;

    private int topMargin = TOP_MARGIN;
    private int leftMargin = LEFT_MARGIN;
    
    public LedGrid() {        
        setBackground(Color.black);
        this.setOpaque(true); // this componenet will be responsible for drawing all pixels
        this.setDoubleBuffered(false);
        setGridParams();
        Timer t = new Timer(UPDATE_MS, this);
        t.start();
    }
    
    private void updateLeds() {
        setGridXY(tmpGridXY);
        if (tmpGridXY.x != oldGridXY.x || tmpGridXY.y != oldGridXY.y){
            setLedRect(repaintAreas, oldGridXY.x, oldGridXY.y);
            int x = tmpGridXY.x;
            int y = tmpGridXY.y;
            setLedRect(repaintArea, x, y);
            repaintAreas.add(repaintArea);
            for (int i=0; i<2; i++){
                x++;
                if (x > 99){
                    x = 0;
                    y++;
                    if (y > 9){
                        y = 0;
                    }
                }
                setLedRect(repaintArea, x, y);
                repaintAreas.add(repaintArea);
            }
            repaint(repaintAreas.x, repaintAreas.y, repaintAreas.width, repaintAreas.height);
        }
    }
    
    @Override
    public void paintComponent(Graphics graphics){
        if (canvas == null || 
                canvasSize.width != getWidth() || canvasSize.height != getHeight()){
            // Get the size of our component
            getSize(canvasSize);
            
            // Set grid parameters
            setGridParams();
            
            // Draw canvas
            drawGrid();
        } else {
            // Clear old LED
            canvasGc.setColor(Color.black);
            canvasGc.fillRect(ledOldRect.x, ledOldRect.y, ledOldRect.width, ledOldRect.height);
        }
        
        // Draw new LED
               
        canvasGc.setColor(Color.white);
        setGridXY(gridXY);
        setLedRect(ledRect, gridXY.x, gridXY.y);
        canvasGc.fillRect(ledRect.x, ledRect.y, ledRect.width, ledRect.height);
        
        // Copy canvas to screen
        graphics.drawImage(canvas, 0, 0, this);
        
        oldGridXY.x = gridXY.x;
        oldGridXY.y = gridXY.y;
        
        ledOldRect.x = ledRect.x;
        ledOldRect.y = ledRect.y;
        ledOldRect.height = ledRect.height;
        ledOldRect.width = ledRect.width;
    }

    private void setGridXY(Point coord){
        setGridXY(coord, 0);
    }
    
    private void setGridXY(Point coord, int timeOffset){
        // round time to the nearest 10 ms
        int time = (int)((5 + System.currentTimeMillis() + timeOffset) % 10_000); // 0 to 9999
        int s = time / 1000; // 0 to 9
        int ms = time - (s * 1000); // 0 to 999
        coord.x = ms / 10; // First 2 ms digits 
        coord.y = s;
    }
    
    private void setLedRect(Rectangle r, int x, int y) {
        r.x = leftMargin + x*ledSize + (x/5) * SPACE5;
        r.y = topMargin + y*ledSize + (y/5) * SPACE5;
        r.width = r.height = ledSize-2;
    }

    private void drawGrid() {
        canvas = createImage(canvasSize.width, canvasSize.height);
        canvasGc = canvas.getGraphics();
        
        canvasGc.setColor(Color.black);
        canvasGc.fillRect(0, 0, getWidth(), getHeight());
        
        Rectangle r = new Rectangle();
        canvasGc.setColor(Color.DARK_GRAY);
        for (int x=0; x<100; x++){
            for (int y=0; y<10; y++){
                setLedRect(r, x, y);
                canvasGc.drawRect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);
            }
        }
        
        FontMetrics fontMetrics = canvasGc.getFontMetrics();
        
        for (int x=0; x<100; x += 10){
            setLedRect(r, x, 0);
            String ms = Integer.toString(x*10);
            int width = fontMetrics.stringWidth(ms);
            canvasGc.drawString(ms, r.x -1 + ledSize/2 - width/2, topMargin - 5);
        }
        setLedRect(r, 100, 0);
        String legend = "time / ms";
        
        int width = fontMetrics.stringWidth(legend);
        canvasGc.drawString(legend, (canvasSize.width - width) / 2, topMargin - (fontMetrics.getHeight() + 5));
    }
    
    @Override
    public Dimension getPreferredSize(){
        return new Dimension(WINDOW_MIN_WIDTH, WINDOW_MIN_HEIGHT);
    }
    
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(WINDOW_MIN_WIDTH, WINDOW_MIN_HEIGHT);
    }  
    
    private void setGridParams() {
        int width = Math.max(canvasSize.width, CANVAS_MIN_WIDTH);
        int height = Math.max(canvasSize.height, CANVAS_MIN_HEIGHT);
        ledSize = ((width - (LEFT_MARGIN + 20*SPACE5)) / 100);
        int usedWidth = 20*SPACE5 + ledSize*100;
        leftMargin = (width - usedWidth) / 2;
        
        int usedHeight = TOP_MARGIN + SPACE5 + ledSize*10;
        topMargin = TOP_MARGIN + Math.max(0, (height - usedHeight) / 2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateLeds();
    }
}

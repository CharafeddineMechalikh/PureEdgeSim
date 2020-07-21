/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Mechalikh
 **/
package com.mechalikh.pureedgesim.SimulationVisualizer;

import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;

public class LineChart extends Chart {

	public LineChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager ) {
		super(title, xAxisTitle, yAxisTitle, simulationManager); 
	}

	protected double[] timeSteps;

	protected void updateHeight(double height) {
		chart.getStyler().setYAxisMax(height);
	}

	@Override
	void update() { 
	}
}

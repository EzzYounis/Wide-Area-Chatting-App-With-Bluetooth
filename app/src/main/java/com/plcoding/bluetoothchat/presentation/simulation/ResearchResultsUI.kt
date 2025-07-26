// ResearchResultsUI.kt - UI Components for Research Data Visualization
package com.plcoding.bluetoothchat.presentation.simulation

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun ResourceComparisonCard(metrics: SimulationViewModel.ResearchMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Resource Usage Comparison",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Memory Usage Bar
            ResourceBar(
                label = "Memory",
                value = metrics.averageMemoryUsage / 1_000_000f,
                maxValue = 100f,
                unit = "MB",
                color = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // CPU Usage Bar
            ResourceBar(
                label = "CPU",
                value = metrics.averageCpuUsage,
                maxValue = 100f,
                unit = "%",
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Battery Impact Bar
            ResourceBar(
                label = "Battery",
                value = metrics.batteryImpact,
                maxValue = 10f,
                unit = "%",
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun ResourceBar(
    label: String,
    value: Float,
    maxValue: Float,
    unit: String,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp)
            Text("${String.format("%.1f", value)}$unit", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Gray.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (value / maxValue).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun SummaryStatisticsCard(metrics: SimulationViewModel.ResearchMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        backgroundColor = Color(0xFFF5F5F5)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    Icons.Default.MailOutline,
                    contentDescription = null,
                    tint = Color(0xFF1976D2)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Research Summary",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "Key Findings:",
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val findings = listOf(
                "✓ Hybrid approach achieved ${String.format("%.1f", metrics.hybridAccuracy * 100)}% accuracy",
                "✓ Average detection time: ${metrics.averageDetectionTime}ms",
                "✓ Successfully detected ${metrics.truePositives} attacks",
                "✓ False positive rate: ${String.format("%.1f", calculateFalsePositiveRate(metrics) * 100)}%"
            )

            findings.forEach { finding ->
                Text(
                    finding,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = Color(0xFF424242)
                )
            }
        }
    }
}

private fun calculateFalsePositiveRate(metrics: SimulationViewModel.ResearchMetrics): Float {
    return if (metrics.falsePositives + metrics.trueNegatives > 0) {
        metrics.falsePositives.toFloat() / (metrics.falsePositives + metrics.trueNegatives)
    } else 0f
}

@Composable
fun ResearchDashboard(
    viewModel: SimulationViewModel,
    modifier: Modifier = Modifier
) {
    val researchMetrics by viewModel.researchMetrics.collectAsState()
    val experimentResults by viewModel.experimentResults.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = Color.White
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Overview") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Detection") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Performance") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Comparison") }
            )
        }

        // Content
        when (selectedTab) {
            0 -> OverviewTab(researchMetrics, viewModel)
            1 -> DetectionMetricsTab(researchMetrics)
            2 -> PerformanceMetricsTab(researchMetrics, experimentResults)
            3 -> ComparisonTab(researchMetrics)
        }
    }
}

@Composable
fun OverviewTab(
    metrics: SimulationViewModel.ResearchMetrics,
    viewModel: SimulationViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Key Metrics Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Accuracy",
                    value = "${String.format("%.1f", metrics.accuracy * 100)}%",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "F1-Score",
                    value = String.format("%.2f", metrics.f1Score),
                    icon = Icons.Default.Email,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // Confusion Matrix
            ConfusionMatrixCard(metrics)
        }

        item {
            // Quick Actions
            ExperimentControlsCard(viewModel)
        }

        item {
            // Recent Results Summary
            RecentResultsCard(metrics)
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.1f),
                            Color.White
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    title,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ConfusionMatrixCard(metrics: SimulationViewModel.ResearchMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Confusion Matrix",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Matrix
            val cellModifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .padding(4.dp)

            Column {
                // Header row
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = cellModifier)
                    MatrixCell("Predicted +", isHeader = true, modifier = cellModifier)
                    MatrixCell("Predicted -", isHeader = true, modifier = cellModifier)
                }

                // Data rows
                Row(modifier = Modifier.fillMaxWidth()) {
                    MatrixCell("Actual +", isHeader = true, modifier = cellModifier)
                    MatrixCell(
                        metrics.truePositives.toString(),
                        backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                        modifier = cellModifier
                    )
                    MatrixCell(
                        metrics.falseNegatives.toString(),
                        backgroundColor = Color(0xFFFF5252).copy(alpha = 0.3f),
                        modifier = cellModifier
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    MatrixCell("Actual -", isHeader = true, modifier = cellModifier)
                    MatrixCell(
                        metrics.falsePositives.toString(),
                        backgroundColor = Color(0xFFFF5252).copy(alpha = 0.3f),
                        modifier = cellModifier
                    )
                    MatrixCell(
                        metrics.trueNegatives.toString(),
                        backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                        modifier = cellModifier
                    )
                }
            }
        }
    }
}

@Composable
fun MatrixCell(
    text: String,
    isHeader: Boolean = false,
    backgroundColor: Color = Color.Transparent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = if (isHeader) Color(0xFFE0E0E0) else backgroundColor,
        shape = RoundedCornerShape(8.dp),
        elevation = if (isHeader) 0.dp else 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (isHeader) 12.sp else 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ExperimentControlsCard(viewModel: SimulationViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Run Experiments",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ExperimentButton(
                        text = "Detection Accuracy",
                        icon = Icons.Default.Search,
                        onClick = {
                            viewModel.runResearchExperiment(
                                SimulationViewModel.ExperimentType.DETECTION_ACCURACY
                            )
                        }
                    )
                }
                item {
                    ExperimentButton(
                        text = "Scalability",
                        icon = Icons.Default.DateRange,
                        onClick = {
                            viewModel.runResearchExperiment(
                                SimulationViewModel.ExperimentType.SCALABILITY_TEST
                            )
                        }
                    )
                }
                item {
                    ExperimentButton(
                        text = "Multi-Hop",
                        icon = Icons.Default.Share,
                        onClick = {
                            viewModel.runResearchExperiment(
                                SimulationViewModel.ExperimentType.MULTI_HOP_PERFORMANCE
                            )
                        }
                    )
                }
                item {
                    ExperimentButton(
                        text = "Hybrid Comparison",
                        icon = Icons.Default.ArrowDropDown,
                        onClick = {
                            viewModel.runResearchExperiment(
                                SimulationViewModel.ExperimentType.HYBRID_COMPARISON
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.exportResultsAsCSV() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export CSV")
                }

                Button(
                    onClick = { viewModel.resetSecurityStates() },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
fun ExperimentButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(48.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun DetectionMetricsTab(metrics: SimulationViewModel.ResearchMetrics) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Detection Rate by Attack Type Chart
            DetectionRateChart(metrics.detectionRateByType)
        }

        item {
            // Confidence by Attack Type
            ConfidenceChart(metrics.averageConfidenceByType)
        }

        item {
            // Performance Metrics Radar Chart
            PerformanceRadarChart(metrics)
        }
    }
}

@Composable
fun DetectionRateChart(detectionRates: Map<String, Float>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Detection Rate by Attack Type",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                drawBarChart(detectionRates)
            }
        }
    }
}

@Composable
fun ConfidenceChart(confidenceByType: Map<String, Float>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Average Confidence by Attack Type",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                drawBarChart(confidenceByType)
            }
        }
    }
}

fun DrawScope.drawBarChart(data: Map<String, Float>) {
    val barWidth = size.width / (data.size * 2 + 1)
    val maxValue = 100f
    val chartHeight = size.height * 0.8f
    var x = barWidth

    data.forEach { (label, value) ->
        val barHeight = (value / maxValue) * chartHeight
        var color = when (label) {
            "SPOOFING" -> Color(0xFFFF9800)
            "INJECTION" -> Color(0xFFF44336)
            "FLOODING" -> Color(0xFF9C27B0)
            "EXPLOIT" -> Color(0xFFE91E63)
            else -> Color.Gray
        }

        // Draw bar
        drawRect(
            color = color,
            topLeft = Offset(x, size.height - barHeight - 40),
            size = Size(barWidth, barHeight)
        )

        // Draw value text
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 30f
                            }
            canvas.nativeCanvas.drawText(
                "${value.toInt()}%",
                x + barWidth / 2,
                size.height - barHeight - 50,
                paint
            )
        }

        x += barWidth * 2
    }
}

@Composable
fun PerformanceRadarChart(metrics: SimulationViewModel.ResearchMetrics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Performance Metrics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val metrics = listOf(
                    "Accuracy" to metrics.accuracy,
                    "Precision" to metrics.precision,
                    "Recall" to metrics.recall,
                    "F1-Score" to metrics.f1Score,
                    "Specificity" to metrics.specificity
                )
                drawRadarChart(metrics)
            }
        }
    }
}

fun DrawScope.drawRadarChart(metrics: List<Pair<String, Float>>) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val radius = minOf(centerX, centerY) * 0.8f
    val angleStep = 360f / metrics.size

    // Draw grid
    for (i in 1..5) {
        val r = radius * (i / 5f)
        drawCircle(
            color = Color.Gray.copy(alpha = 0.2f),
            radius = r,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // Draw axes
    metrics.forEachIndexed { index, _ ->
        val angle = Math.toRadians((angleStep * index - 90).toDouble())
        val endX = centerX + radius * cos(angle).toFloat()
        val endY = centerY + radius * sin(angle).toFloat()

        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(centerX, centerY),
            end = Offset(endX, endY),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Draw data
    val path = Path()
    metrics.forEachIndexed { index, (_, value) ->
        val angle = Math.toRadians((angleStep * index - 90).toDouble())
        val r = radius * value
        val x = centerX + r * cos(angle).toFloat()
        val y = centerY + r * sin(angle).toFloat()

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(
        path = path,
        color = Color(0xFF2196F3).copy(alpha = 0.3f)
    )
    drawPath(
        path = path,
        color = Color(0xFF2196F3),
        style = Stroke(width = 2.dp.toPx())
    )
}

@Composable
fun PerformanceMetricsTab(
    metrics: SimulationViewModel.ResearchMetrics,
    experimentResults: List<SimulationViewModel.ExperimentResult>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Performance Timeline
            PerformanceTimelineCard(experimentResults)
        }

        item {
            // Detection Time Statistics
            DetectionTimeCard(metrics)
        }

        item {
            // Resource Usage Card
            ResourceUsageCard(metrics)
        }
    }
}

@Composable
fun PerformanceTimelineCard(results: List<SimulationViewModel.ExperimentResult>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Detection Rate Over Time",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No experiment data yet", color = Color.Gray)
                }
            } else {
                // Simple line chart placeholder
                Text("Detection rates: ${results.map { "${it.detectionRate * 100}%" }.joinToString()}")
            }
        }
    }
}

@Composable
fun DetectionTimeCard(metrics: SimulationViewModel.ResearchMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Detection Time Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val timeMetrics = listOf(
                "Average" to "${metrics.averageDetectionTime}ms",
                "Minimum" to "${if (metrics.minDetectionTime == Long.MAX_VALUE) 0 else metrics.minDetectionTime}ms",
                "Maximum" to "${metrics.maxDetectionTime}ms"
            )

            timeMetrics.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray)
                    Text(value, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ResourceUsageCard(metrics: SimulationViewModel.ResearchMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Resource Consumption",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val resourceMetrics = listOf(
                "Avg Memory" to "${metrics.averageMemoryUsage / 1_000_000}MB",
                "Peak Memory" to "${metrics.peakMemoryUsage / 1_000_000}MB",
                "CPU Usage" to "${String.format("%.1f", metrics.averageCpuUsage)}%",
                "Battery Impact" to "${String.format("%.2f", metrics.batteryImpact)}%"
            )

            resourceMetrics.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray)
                    Text(value, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ComparisonTab(metrics: SimulationViewModel.ResearchMetrics) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Hybrid vs Traditional Comparison
            HybridComparisonCard(metrics)
        }

        item {
            // Resource Consumption Comparison
            ResourceComparisonCard(metrics)
        }

        item {
            // Summary Statistics
            SummaryStatisticsCard(metrics)
        }
    }
}

@Composable
fun HybridComparisonCard(metrics: SimulationViewModel.ResearchMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Hybrid Approach Comparison",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val approaches = listOf(
                Triple("ML-Only", metrics.mlAccuracy, Color(0xFF2196F3)),
                Triple("Rule-Based", metrics.ruleBasedAccuracy, Color(0xFF4CAF50)),
                Triple("Hybrid (Ours)", metrics.hybridAccuracy, Color(0xFFFF9800))
            )

            approaches.forEach { (name, accuracy, color) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        name,
                        modifier = Modifier.width(120.dp),
                        fontWeight = FontWeight.Medium
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Gray.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(accuracy)
                                .clip(RoundedCornerShape(12.dp))
                                .background(color)
                        )
                    }

                    Text(
                        "${String.format("%.1f", accuracy * 100)}%",
                        modifier = Modifier.width(60.dp),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Improvement calculation
            val improvement = ((metrics.hybridAccuracy - maxOf(metrics.mlAccuracy, metrics.ruleBasedAccuracy)) /
                    maxOf(metrics.mlAccuracy, metrics.ruleBasedAccuracy)) * 100

            if (improvement > 0) {
                Card(
                    backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Hybrid approach shows ${String.format("%.1f", improvement)}% improvement",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentResultsCard(metrics: SimulationViewModel.ResearchMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Latest Results Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val results = listOf(
                "Average Detection Time" to "${metrics.averageDetectionTime}ms",
                "Message Delivery Rate" to "${String.format("%.1f", metrics.messageDeliveryRate * 100)}%",
                "False Positive Rate" to "${String.format("%.1f",
                    if (metrics.falsePositives + metrics.trueNegatives > 0)
                        (metrics.falsePositives.toFloat() / (metrics.falsePositives + metrics.trueNegatives)) * 100
                    else 0f)}%",
                "Average Hop Count" to String.format("%.1f", metrics.averageHopCount)
            )

            results.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray)
                    Text(value, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
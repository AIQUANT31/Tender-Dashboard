import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivityReportService, SummaryReport } from '../../services/activity-report.service';

@Component({
  selector: 'app-summary-report',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './summary-report.component.html',
  styleUrl: './summary-report.component.css'
})
export class SummaryReportComponent implements OnInit {
  @Input() bidId?: number;
  @Input() tenderId?: number;
  
  report: SummaryReport | null = null;
  loading = false;
  error = '';

  constructor(private activityReportService: ActivityReportService) {}

  ngOnInit(): void {
    this.loadReport();
  }

  loadReport(): void {
    this.loading = true;
    this.error = '';

    const request = this.bidId 
      ? this.activityReportService.getBidSummaryReport(this.bidId)
      : this.activityReportService.getTenderSummaryReport(this.tenderId!);

    request.subscribe({
      next: (response) => {
        if (response.success && response.report) {
          this.report = response.report;
        } else {
          this.error = response.message || 'Failed to load report';
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error loading report: ' + err.message;
        this.loading = false;
      }
    });
  }

  getScoreClass(score: number): string {
    if (score >= 80) return 'score-excellent';
    if (score >= 60) return 'score-good';
    if (score >= 40) return 'score-average';
    return 'score-poor';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return 'N/A';
    const date = new Date(dateStr);
    return date.toLocaleString();
  }

  // Get all documents combined into one list
  getAllReportDocuments(): string[] {
    const allDocs: string[] = [];
    if (this.report) {
      if (this.report.documentsFound) {
        allDocs.push(...this.report.documentsFound);
      }
      if (this.report.missingDocuments) {
        allDocs.push(...this.report.missingDocuments);
      }
      if (this.report.duplicateDocuments) {
        allDocs.push(...this.report.duplicateDocuments);
      }
    }
    return allDocs;
  }
}

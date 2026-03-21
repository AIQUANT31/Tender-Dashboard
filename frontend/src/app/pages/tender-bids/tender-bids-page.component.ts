import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { NavComponent } from '../../components/nav/nav.component';
import { TenderBidsComponent } from '../../components/tender-bids/tender-bids.component';

interface TenderInfo {
  id: number;
  name: string;
  budget: number;
  status: string;
}

@Component({
  selector: 'app-tender-bids-page',
  standalone: true,
  imports: [CommonModule, RouterModule, NavComponent, TenderBidsComponent],
  template: `
    <div class="bids-page-container">
      <app-nav></app-nav>
      
      <main class="bids-page-content">
        <div class="page-header">
          <button class="back-btn" (click)="goBack()">← Back to Tender</button>
        </div>
        
        <app-tender-bids
          *ngIf="tender"
          [tender]="tender"
          (onClose)="goBack()">
        </app-tender-bids>
        
        <div class="loading-container" *ngIf="loading">
          <div class="spinner"></div>
          <p>Loading tender details...</p>
        </div>
        
        <div class="error-container" *ngIf="error && !loading">
          <p class="error-message">{{ error }}</p>
          <button class="retry-btn" (click)="loadTender()">Retry</button>
        </div>
      </main>
    </div>
  `,
  styles: [`
    .bids-page-container {
      min-height: 100vh;
      background: #f5f7fa;
    }
    
    .bids-page-content {
      padding: 30px;
      max-width: 1200px;
      margin: 0 auto;
    }
    
    .page-header {
      margin-bottom: 24px;
    }
    
    .back-btn {
      background: #6366f1;
      color: white;
      border: none;
      padding: 12px 24px;
      border-radius: 10px;
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }
    
    .back-btn:hover {
      background: #4f46e5;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
    }
    
    .loading-container {
      padding: 60px;
      text-align: center;
    }
    
    .spinner {
      width: 50px;
      height: 50px;
      border: 4px solid #e2e8f0;
      border-top-color: #6366f1;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 0 auto 20px;
    }
    
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    
    .error-container {
      padding: 60px;
      text-align: center;
      background: white;
      border-radius: 16px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    }
    
    .error-message {
      color: #ef4444;
      margin-bottom: 20px;
      font-size: 1.1rem;
    }
    
    .retry-btn {
      background: #6366f1;
      color: white;
      border: none;
      padding: 12px 28px;
      border-radius: 10px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    
    .retry-btn:hover {
      background: #4f46e5;
    }
  `]
})
export class TenderBidsPageComponent implements OnInit {
  tender: TenderInfo | null = null;
  loading = false;
  error = '';
  
  constructor(
    private http: HttpClient, 
    private cdr: ChangeDetectorRef, 
    private router: Router,
    private route: ActivatedRoute
  ) {}
  
  ngOnInit() {
    this.loadTender();
  }
  
  loadTender() {
    this.loading = true;
    this.error = '';
    
    const tenderId = this.route.snapshot.paramMap.get('tenderId');
    
    if (!tenderId) {
      this.error = 'Tender ID not found';
      this.loading = false;
      return;
    }
    
    this.http.get<TenderInfo>(`http://localhost:8080/api/tenders/${tenderId}`).subscribe({
      next: (data) => {
        this.tender = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading tender:', err);
        this.error = 'Error loading tender details';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }
  
  goBack() {
    const tenderId = this.route.snapshot.paramMap.get('tenderId');
    if (tenderId) {
      this.router.navigate(['/tender', tenderId]);
    } else {
      this.router.navigate(['/tender']);
    }
  }
}

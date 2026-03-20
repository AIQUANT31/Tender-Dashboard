import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, finalize, map, tap } from 'rxjs/operators';


export interface ZipDownloadResponse {
  success: boolean;
  message: string;
  fileName?: string;
}

export interface DownloadConfig {
  apiUrl: string;
  timeout?: number;
}


@Component({
  selector: 'app-bid-zip-download',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bid-zip-download.component.html',
  styleUrl: './bid-zip-download.component.css'
})
export class BidZipDownloadComponent implements OnInit {
  // Required inputs
  @Input() bidId: number | null = null;
  @Input() zipFilePath: string | null = null;

  // Optional configuration
  @Input() apiUrl: string = 'http://localhost:8080/api/bids';
  @Input() autoDownload: boolean = false;


  @Output() zipDownloaded = new EventEmitter<string>();
  @Output() downloadStarted = new EventEmitter<number>();
  @Output() downloadError = new EventEmitter<string>();
  @Output() downloadProgress = new EventEmitter<number>();

  // Component state
  isLoading: boolean = false;
  errorMessage: string | null = null;
  showSuccessMessage: boolean = false;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    if (this.autoDownload && this.bidId && this.zipFilePath) {
      this.downloadZip();
    }
  }

 
  getFileName(path: string | null): string {
    if (!path) return 'Documents';
    return path.substring(path.lastIndexOf('/') + 1);
  }

  
  private getFileExtension(fileName: string): string {
    const lastDotIndex = fileName.lastIndexOf('.');
    return lastDotIndex !== -1 ? fileName.substring(lastDotIndex + 1).toLowerCase() : '';
  }

  
  private validateInputs(): boolean {
    if (!this.bidId) {
      this.errorMessage = 'Bid ID is required for download';
      this.downloadError.emit(this.errorMessage);
      return false;
    }

    if (!this.zipFilePath) {
      this.errorMessage = 'ZIP file path is required';
      this.downloadError.emit(this.errorMessage);
      return false;
    }

    const fileName = this.getFileName(this.zipFilePath);
    if (!fileName.toLowerCase().endsWith('.zip')) {
      this.errorMessage = 'Invalid file type. Expected a ZIP file';
      this.downloadError.emit(this.errorMessage);
      return false;
    }

    return true;
  }

  
  private createDownloadUrl(): string {
    const fileName = this.getFileName(this.zipFilePath);
    return `${this.apiUrl}/download-zip?fileName=${encodeURIComponent(fileName)}`;
  }

 
  downloadZip(): void {
    // Reset state
    this.errorMessage = null;
    this.showSuccessMessage = false;

    // Validate inputs
    if (!this.validateInputs()) {
      return;
    }

    this.isLoading = true;
    this.downloadStarted.emit(this.bidId as number);

    const url = this.createDownloadUrl();
    
    // Set up HTTP options for blob download
    const options: {
      responseType: 'blob';
      headers: HttpHeaders;
    } = {
      responseType: 'blob',
      headers: new HttpHeaders({
        'Accept': 'application/zip, application/octet-stream'
      })
    };

    // Make the HTTP request
    this.http.get(url, options).pipe(
      tap(() => {
        this.downloadProgress.emit(100);
      }),
      map((blob: Blob) => {
        return this.processDownload(blob);
      }),
      catchError((error) => {
        return this.handleError(error);
      }),
      finalize(() => {
        this.isLoading = false;
      })
    ).subscribe({
      next: (success) => {
        if (success) {
          this.showSuccessMessage = true;
          this.zipDownloaded.emit(this.zipFilePath as string);
          
          // Hide success message after 3 seconds
          setTimeout(() => {
            this.showSuccessMessage = false;
          }, 3000);
        }
      },
      error: (error) => {
        // Error is already handled in catchError
        console.error('Download error:', error);
      }
    });
  }

  
  private processDownload(blob: Blob): boolean {
    try {
      const fileName = this.getFileName(this.zipFilePath);
      
      // Create a blob URL
      const blobUrl = window.URL.createObjectURL(blob);
      
      // Create a temporary anchor element for download
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = fileName;
      link.style.display = 'none';
      
      // Append to body, click, and remove
      document.body.appendChild(link);
      link.click();
      
      // Clean up
      setTimeout(() => {
        document.body.removeChild(link);
        window.URL.revokeObjectURL(blobUrl);
      }, 100);

      return true;
    } catch (error) {
      console.error('Error processing download:', error);
      this.errorMessage = 'Failed to process the downloaded file';
      this.downloadError.emit(this.errorMessage);
      return false;
    }
  }


  private handleError(error: unknown): Observable<never> {
    let errorMessage = 'An error occurred during download';

    if (error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.message}`;
    } else {
      // Server-side error
      const httpError = error as { status?: number; message?: string };
      
      switch (httpError.status) {
        case 404:
          errorMessage = 'ZIP file not found on the server';
          break;
        case 403:
          errorMessage = 'Access denied. You do not have permission to download this file';
          break;
        case 401:
          errorMessage = 'Authentication required. Please log in again';
          break;
        case 500:
          errorMessage = 'Server error. Please try again later';
          break;
        default:
          errorMessage = httpError.message || `Error code: ${httpError.status}`;
      }
    }

    this.errorMessage = errorMessage;
    this.downloadError.emit(errorMessage);
    
    return throwError(() => new Error(errorMessage));
  }

 
  downloadZipViaWindowOpen(): void {
    this.errorMessage = null;
    this.showSuccessMessage = false;

    if (!this.validateInputs()) {
      return;
    }

    this.isLoading = true;
    this.downloadStarted.emit(this.bidId as number);

    try {
      const fileName = this.getFileName(this.zipFilePath);
      const url = `${this.apiUrl}/download-zip?fileName=${encodeURIComponent(fileName)}`;
      
      
      window.open(url, '_blank');
      
      this.showSuccessMessage = true;
      this.zipDownloaded.emit(this.zipFilePath as string);
      
      setTimeout(() => {
        this.showSuccessMessage = false;
        this.isLoading = false;
      }, 3000);
      
    } catch (error) {
      this.isLoading = false;
      this.errorMessage = 'Failed to open download window';
      this.downloadError.emit(this.errorMessage);
    }
  }

 
  clearError(): void {
    this.errorMessage = null;
  }

  
  clearSuccess(): void {
    this.showSuccessMessage = false;
  }

 
  reset(): void {
    this.isLoading = false;
    this.errorMessage = null;
    this.showSuccessMessage = false;
  }
}

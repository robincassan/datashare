import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FileService } from '../../services/file.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  imports: [FormsModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardComponent implements OnInit {
  files: any[] = [];
  selectedFile: File | null = null;
  password = '';
  expiresAt = '';
  error = '';
  success = '';
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  constructor(private fileService: FileService, private authService: AuthService, private router: Router) {}

  ngOnInit() { this.loadFiles(); }

  loadFiles() {
    this.fileService.list().subscribe(files => this.files = files);
  }

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
  }

  upload() {
    if (!this.selectedFile) return;
    const formData = new FormData();
    formData.append('file', this.selectedFile);
    if (this.password) formData.append('password', this.password);
    if (this.expiresAt) formData.append('expiresAt', this.expiresAt);
    this.fileService.upload(formData).subscribe({
      next: () => {
        this.success = 'Fichier uploadé !';
        this.selectedFile = null;
        this.password = '';
        this.expiresAt = '';
        this.fileInput.nativeElement.value = '';
        this.loadFiles();
      },
      error: (err) => this.error = err.error || "Erreur lors de l'upload"
    });
  }

  deleteFile(id: string) {
    this.fileService.delete(id).subscribe({
      next: () => this.loadFiles(),
      error: () => this.error = "Erreur lors de la suppression"
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getDownloadLink(token: string): string {
    return `http://localhost:4200/download/${token}`;
  }

  copyLink(token: string) {
    navigator.clipboard.writeText(this.getDownloadLink(token));
    this.success = 'Lien copié !';
  }
}

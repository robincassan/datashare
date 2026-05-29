  import { Component, OnInit } from '@angular/core';
  import { ActivatedRoute } from '@angular/router';
  import { FormsModule } from '@angular/forms';
  import { FileService } from '../../services/file.service';

  @Component({
    selector: 'app-download',
    imports: [FormsModule],
    templateUrl: './download.html',
    styleUrls: ['./download.css']
  })
  export class DownloadComponent implements OnInit {
    fileInfo: any = null;
    token = '';
    password = '';
    error = '';
    passwordRequired = false;

    constructor(private route: ActivatedRoute, private fileService: FileService) {}

    ngOnInit() {
      this.token = this.route.snapshot.paramMap.get('token') || '';
      this.loadInfo();
    }

    loadInfo() {
      this.fileService.getInfo(this.token).subscribe({
        next: (info) => {
          this.fileInfo = info;
          this.passwordRequired = info.hasPassword;
        },
        error: () => {
          this.error = 'Fichier introuvable ou expiré';
        }
      });
    }

    download() {
      this.fileService.download(this.token, this.passwordRequired ? this.password : undefined).subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = this.fileInfo.fileName;
          a.click();
          window.URL.revokeObjectURL(url);
        },
        error: () => {
          this.error = 'Mot de passe incorrect';
        }
      });
    }
  }
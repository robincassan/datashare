  import { Injectable } from '@angular/core';
  import { HttpClient } from '@angular/common/http';
  import { Observable } from 'rxjs';

  @Injectable({ providedIn: 'root' })
  export class FileService {
    private apiUrl = 'http://localhost:8080/api/files';

    constructor(private http: HttpClient) {}

    upload(formData: FormData): Observable<any> {
      return this.http.post(`${this.apiUrl}/upload`, formData);
    }

    list(): Observable<any[]> {
      return this.http.get<any[]>(this.apiUrl);
    }

    delete(id: string): Observable<void> {
      return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }

    getInfo(token: string): Observable<any> {
      return this.http.get(`${this.apiUrl}/${token}`);
    }

    download(token: string, password?: string): Observable<Blob> {
      return this.http.post(`${this.apiUrl}/${token}/download`, { password }, { responseType: 'blob' });
    }
  }
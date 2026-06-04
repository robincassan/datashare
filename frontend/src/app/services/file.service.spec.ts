import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { FileService } from './file.service';

describe('FileService', () => {
  let service: FileService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [FileService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(FileService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should upload a file', () => {
    const formData = new FormData();
    formData.append('file', new Blob(['test']));

    service.upload(formData).subscribe(res => {
      expect(res.fileName).toBe('test.txt');
    });

    const req = httpMock.expectOne('http://localhost:8080/api/files/upload');
    expect(req.request.method).toBe('POST');
    req.flush({ fileName: 'test.txt' });
  });

  it('should list files', () => {
    const mockFiles = [{ id: '1', fileName: 'test.txt' }];

    service.list().subscribe(files => {
      expect(files.length).toBe(1);
      expect(files[0].fileName).toBe('test.txt');
    });

    const req = httpMock.expectOne('http://localhost:8080/api/files');
    expect(req.request.method).toBe('GET');
    req.flush(mockFiles);
  });

  it('should delete a file', () => {
    service.delete('file-1').subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/files/file-1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should get file info', () => {
    service.getInfo('token-123').subscribe(info => {
      expect(info.fileName).toBe('test.txt');
    });

    const req = httpMock.expectOne('http://localhost:8080/api/files/token-123');
    expect(req.request.method).toBe('GET');
    req.flush({ fileName: 'test.txt' });
  });

  it('should download a file', () => {
    const blob = new Blob(['test'], { type: 'text/plain' });

    service.download('token-123').subscribe(result => {
      expect(result).toBe(blob);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/files/token-123/download');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ password: undefined });
    req.flush(blob);
  });

  it('should download a file with password', () => {
    const blob = new Blob(['test'], { type: 'text/plain' });

    service.download('token-123', 'secret').subscribe(result => {
      expect(result).toBe(blob);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/files/token-123/download');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ password: 'secret' });
    req.flush(blob);
  });
});

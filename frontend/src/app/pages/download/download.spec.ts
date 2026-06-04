import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { DownloadComponent } from './download';
import { FileService } from '../../services/file.service';
import { of, throwError, Subject } from 'rxjs';

describe('DownloadComponent', () => {
  let component: DownloadComponent;
  let fixture: any;

  beforeEach(async () => {
    const fileServiceMock = {
      getInfo: vi.fn(),
      download: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [DownloadComponent],
      providers: [
        { provide: FileService, useValue: fileServiceMock },
        provideRouter([]),
        provideHttpClient()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DownloadComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have empty initial state', () => {
    expect(component.token).toBe('');
    expect(component.fileInfo).toBeNull();
    expect(component.error).toBe('');
    expect(component.passwordRequired).toBeFalsy();
  });

  it('should load file info', () => {
    const fileService = TestBed.inject(FileService) as any;
    const mockInfo = { fileName: 'test.txt', fileSize: 1024, hasPassword: false };
    fileService.getInfo.mockReturnValue(of(mockInfo));

    component.token = 'token-123';
    component.loadInfo();

    expect(component.fileInfo).toEqual(mockInfo);
    expect(component.passwordRequired).toBeFalsy();
  });

  it('should set passwordRequired when file has password', () => {
    const fileService = TestBed.inject(FileService) as any;
    fileService.getInfo.mockReturnValue(of({ hasPassword: true }));

    component.token = 'token-123';
    component.loadInfo();

    expect(component.passwordRequired).toBeTruthy();
  });

  it('should set error when file not found', () => {
    const fileService = TestBed.inject(FileService) as any;
    fileService.getInfo.mockReturnValue(throwError(() => new Error('Not found')));

    component.token = 'invalid';
    component.loadInfo();

    expect(component.error).toBe('Fichier introuvable ou expiré');
  });

  it('should download file without password', () => {
    const fileService = TestBed.inject(FileService) as any;
    const blob = new Blob(['test'], { type: 'text/plain' });
    component.fileInfo = { fileName: 'test.txt' };
    component.passwordRequired = false;
    component.token = 'token-123';
    fileService.download.mockReturnValue(of(blob));
    vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:url');

    component.download();

    expect(fileService.download).toHaveBeenCalledWith('token-123', undefined);
  });

  it('should download file with password', () => {
    const fileService = TestBed.inject(FileService) as any;
    const blob = new Blob(['test'], { type: 'text/plain' });
    component.fileInfo = { fileName: 'test.txt' };
    component.passwordRequired = true;
    component.password = 'secret';
    component.token = 'token-123';
    fileService.download.mockReturnValue(of(blob));
    vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:url');

    component.download();

    expect(fileService.download).toHaveBeenCalledWith('token-123', 'secret');
  });

  it('should set error on download failure', () => {
    const fileService = TestBed.inject(FileService) as any;
    component.fileInfo = { fileName: 'test.txt' };
    component.token = 'token-123';
    fileService.download.mockReturnValue(throwError(() => new Error('Failed')));

    component.download();

    expect(component.error).toBe('Mot de passe incorrect');
  });

  it('should show loading state while fetching file info', () => {
    const fileService = TestBed.inject(FileService) as any;
    const subject = new Subject<any>();
    fileService.getInfo.mockReturnValue(subject);

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Chargement...');
    subject.complete();
  });

  it('should display file info after loading', () => {
    const fileService = TestBed.inject(FileService) as any;
    fileService.getInfo.mockReturnValue(of({ fileName: 'test.txt', fileSize: 1024, expiresAt: '2025-01-01' }));
    component.token = 'token-123';
    component.loadInfo();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('test.txt');
    expect(fixture.nativeElement.textContent).toContain('1024 octets');
  });

  it('should show error message in the template', () => {
    const fileService = TestBed.inject(FileService) as any;
    fileService.getInfo.mockReturnValue(throwError(() => new Error('Not found')));
    component.token = 'invalid';
    component.loadInfo();
    fixture.detectChanges();

    const errorEl = fixture.nativeElement.querySelector('.error');
    expect(errorEl).toBeTruthy();
    expect(errorEl.textContent).toContain('Fichier introuvable ou expiré');
  });

  it('should show password input when file requires password', () => {
    const fileService = TestBed.inject(FileService) as any;
    fileService.getInfo.mockReturnValue(of({ fileName: 'test.txt', hasPassword: true }));
    component.token = 'token-123';
    component.loadInfo();
    fixture.detectChanges();

    const passwordInput = fixture.nativeElement.querySelector('input[type="password"]');
    expect(passwordInput).toBeTruthy();
  });
});

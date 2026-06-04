import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { DashboardComponent } from './dashboard';
import { FileService } from '../../services/file.service';
import { AuthService } from '../../services/auth.service';
import { of, throwError } from 'rxjs';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: any;

  beforeEach(async () => {
    const fileServiceMock = {
      list: vi.fn().mockReturnValue(of([])),
      upload: vi.fn(),
      delete: vi.fn()
    };
    const authServiceMock = {
      logout: vi.fn()
    };
    const routerMock = {
      navigate: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: FileService, useValue: fileServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have empty initial state', () => {
    expect(component.files).toEqual([]);
    expect(component.selectedFile).toBeNull();
    expect(component.error).toBe('');
    expect(component.success).toBe('');
  });

  it('should return correct download link', () => {
    expect(component.getDownloadLink('token-123')).toBe('http://localhost:4200/download/token-123');
  });

  it('should load files on init', () => {
    const fileService = TestBed.inject(FileService) as any;
    fileService.list.mockReturnValue(of([{ id: '1', fileName: 'test.txt' }]));
    component.ngOnInit();
    expect(component.files.length).toBe(1);
    expect(component.files[0].fileName).toBe('test.txt');
  });

  it('should upload file and reload list on success', () => {
    const fileService = TestBed.inject(FileService) as any;
    const file = new File(['test'], 'test.txt', { type: 'text/plain' });
    component.selectedFile = file;
    component.fileInput = { nativeElement: { value: '' } } as any;
    fileService.upload.mockReturnValue(of({}));
    fileService.list.mockReturnValue(of([]));

    component.upload();

    expect(fileService.upload).toHaveBeenCalled();
    expect(component.success).toBe('Fichier uploadé !');
    expect(component.selectedFile).toBeNull();
  });

  it('should not upload when no file selected', () => {
    const fileService = TestBed.inject(FileService) as any;
    component.selectedFile = null;
    component.upload();
    expect(fileService.upload).not.toHaveBeenCalled();
  });

  it('should set error on upload failure', () => {
    const fileService = TestBed.inject(FileService) as any;
    component.selectedFile = new File(['test'], 'test.txt', { type: 'text/plain' });
    fileService.upload.mockReturnValue(throwError(() => ({ error: 'Upload failed' })));

    component.upload();

    expect(component.error).toBeTruthy();
  });

  it('should delete file and reload list', () => {
    const fileService = TestBed.inject(FileService) as any;
    fileService.delete.mockReturnValue(of(void 0));
    fileService.list.mockReturnValue(of([]));

    component.deleteFile('file-1');

    expect(fileService.delete).toHaveBeenCalledWith('file-1');
  });

  it('should logout and navigate to login', () => {
    const router = TestBed.inject(Router) as any;
    component.logout();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should set selectedFile on file selection', () => {
    const file = new File(['test'], 'test.txt', { type: 'text/plain' });
    const event = { target: { files: [file] } };
    component.onFileSelected(event);
    expect(component.selectedFile).toBe(file);
  });

  it('should copy link and show success', () => {
    Object.assign(navigator, { clipboard: { writeText: vi.fn() } });
    component.copyLink('token-123');
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('http://localhost:4200/download/token-123');
    expect(component.success).toBe('Lien copié !');
  });

  it('should render the dashboard heading', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Mes fichiers');
  });

  it('should show empty state when no files', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucun fichier');
  });

  it('should show success message in the template', () => {
    component.success = 'Fichier uploadé !';
    fixture.detectChanges();
    const successEl = fixture.nativeElement.querySelector('.success');
    expect(successEl).toBeTruthy();
    expect(successEl.textContent).toContain('Fichier uploadé !');
  });

  it('should show error message in the template', () => {
    component.error = 'Upload failed';
    fixture.detectChanges();
    const errorEl = fixture.nativeElement.querySelector('.error');
    expect(errorEl).toBeTruthy();
    expect(errorEl.textContent).toContain('Upload failed');
  });

  it('should render files in the list', () => {
    const fileService = TestBed.inject(FileService) as any;
    fileService.list.mockReturnValue(of([
      { id: '1', fileName: 'test1.txt', fileSize: 100, status: 'ACTIVE', expiresAt: '2025-01-01', downloadToken: 'token-1' },
      { id: '2', fileName: 'test2.txt', fileSize: 200, status: 'ACTIVE', expiresAt: '2025-02-01', downloadToken: 'token-2' }
    ]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('test1.txt');
    expect(fixture.nativeElement.textContent).toContain('test2.txt');
    expect(fixture.nativeElement.textContent).not.toContain('Aucun fichier');
  });
});

import { NgModule } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatToolbarModule } from '@angular/material/toolbar';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { ConfirmationDialog } from './dialogs/confirmation/confirmation-dialog.component';
import { HeaderComponent } from './header/header.component';
import { FooterComponent } from './footer/footer.component';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './pages/login/login.component';
import { MessageDialog } from './dialogs/message/message-dialog.component';
import { SimpleTextComponent } from './text/simple/simple-text.component';
import { EnvironmentComponent } from './pages/environment/environment.component';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { SandboxComponent } from './pages/sandbox/sandbox.component';
import { ScenarioComponent } from './pages/scenario/scenario.component';
import { CreateSandboxComponent } from './pages/create-sandbox/create-sandbox.component';
import { EditSandboxComponent } from './pages/edit-sandbox/edit-sandbox.component';
import { ReportComponent } from './pages/report/report.component';
import {TextWaitingComponent} from "./text/waiting/text-waiting.component";
import {EditHeaderComponent} from "./pages/edit-header/edit-header.component";

@NgModule({ declarations: [
        AppComponent,
        ConfirmationDialog,
        CreateSandboxComponent,
        EditHeaderComponent,
        EditSandboxComponent,
        EnvironmentComponent,
        FooterComponent,
        HeaderComponent,
        HomeComponent,
        LoginComponent,
        MessageDialog,
        ReportComponent,
        SandboxComponent,
        ScenarioComponent,
        SimpleTextComponent,
        TextWaitingComponent,
    ],
    bootstrap: [AppComponent], imports: [BrowserAnimationsModule,
        BrowserModule,
        FormsModule,
        MatButtonModule,
        MatCardModule,
        MatCheckboxModule,
        MatDialogModule,
        MatIconModule,
        MatInputModule,
        MatMenuModule,
        MatProgressSpinnerModule,
        MatSelectModule,
        MatToolbarModule,
        ReactiveFormsModule,
        // import as last module!
        AppRoutingModule], providers: [provideHttpClient(withInterceptorsFromDi())] })
export class AppModule { }
